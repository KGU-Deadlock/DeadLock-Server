package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.kafka.JacksonDeserializer;
import com.deadlock.hellocs.global.kafka.JacksonSerializer;
import com.deadlock.hellocs.shared.events.GradingCompletedEventMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정.
 *
 * 재시도 전략:
 *   - 1초 간격으로 최대 3회 재시도
 *   - 3회 실패 시 Dead Letter Topic(grading.completed.DLT)으로 발행
 *
 * DLT 활용:
 *   - 별도 모니터링 잡 또는 수동으로 grading.completed.DLT 메시지를 확인/재처리
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ─── Consumer ───────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, GradingCompletedEventMessage> gradingEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new JacksonDeserializer<>(GradingCompletedEventMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GradingCompletedEventMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GradingCompletedEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(gradingEventConsumerFactory());

        // 3회 재시도 후 DLT로 발행
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(dltKafkaTemplate()),
                new FixedBackOff(1000L, 3)
        );
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // ─── DLT Producer ───────────────────────────────────────────────────────

    /**
     * Dead Letter Topic 발행용 KafkaTemplate.
     * DLT 토픽명은 Spring이 자동으로 "{원본토픽}.DLT" 로 설정 (grading.completed.DLT).
     */
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate() {
        return new KafkaTemplate<>(dltProducerFactory());
    }

    @Bean
    public ProducerFactory<String, Object> dltProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonSerializer<>());
    }
}
