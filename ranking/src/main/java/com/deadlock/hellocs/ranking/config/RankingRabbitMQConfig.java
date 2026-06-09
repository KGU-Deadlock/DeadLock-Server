package com.deadlock.hellocs.ranking.config;

import com.deadlock.hellocs.common.amqp.RabbitMQConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RankingRabbitMQConfig {

    public static final String QUEUE_RANKING_GRADING = "ranking.grading.completed";

    @Bean
    Queue rankingGradingQueue() {
        return QueueBuilder.durable(QUEUE_RANKING_GRADING).build();
    }

    @Bean
    Binding rankingGradingBinding(Queue rankingGradingQueue, TopicExchange hellocsEventsExchange) {
        return BindingBuilder.bind(rankingGradingQueue).to(hellocsEventsExchange).with(RabbitMQConfig.ROUTING_KEY_GRADING);
    }
}
