package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.kafka.JacksonSerializer;
import com.deadlock.hellocs.shared.events.GradingCompletedEventMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, GradingCompletedEventMessage> gradingEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonSerializer<>());
    }

    @Bean
    public KafkaTemplate<String, GradingCompletedEventMessage> gradingEventKafkaTemplate() {
        return new KafkaTemplate<>(gradingEventProducerFactory());
    }
}
