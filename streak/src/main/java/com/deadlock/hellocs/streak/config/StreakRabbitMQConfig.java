package com.deadlock.hellocs.streak.config;

import com.deadlock.hellocs.common.amqp.RabbitMQConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreakRabbitMQConfig {

    public static final String QUEUE_STREAK_GRADING = "streak.grading.completed";
    public static final String QUEUE_STREAK_INTERVIEW = "streak.interview.completed";

    @Bean
    Queue streakGradingQueue() {
        return QueueBuilder.durable(QUEUE_STREAK_GRADING).build();
    }

    @Bean
    Queue streakInterviewQueue() {
        return QueueBuilder.durable(QUEUE_STREAK_INTERVIEW).build();
    }

    @Bean
    Binding streakGradingBinding(Queue streakGradingQueue, TopicExchange hellocsEventsExchange) {
        return BindingBuilder.bind(streakGradingQueue).to(hellocsEventsExchange).with(RabbitMQConfig.ROUTING_KEY_GRADING);
    }

    @Bean
    Binding streakInterviewBinding(Queue streakInterviewQueue, TopicExchange hellocsEventsExchange) {
        return BindingBuilder.bind(streakInterviewQueue).to(hellocsEventsExchange).with(RabbitMQConfig.ROUTING_KEY_INTERVIEW);
    }
}
