package com.deadlock.hellocs.common.amqp;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RabbitMQConfig {

    public static final String EXCHANGE = "hellocs.events";
    public static final String ROUTING_KEY_GRADING = "grading.completed";
    public static final String ROUTING_KEY_INTERVIEW = "interview.completed";

    @Bean
    TopicExchange hellocsEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
