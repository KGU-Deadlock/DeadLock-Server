package com.deadlock.hellocs.grading.adapter.out.event;

import com.deadlock.hellocs.grading.application.port.out.CommandGradingEventOutputPort;
import com.deadlock.hellocs.common.amqp.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradingEventPublishAdapter implements CommandGradingEventOutputPort {

    private static final String ROUTING_KEY = "grading.completed";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(GradingCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, ROUTING_KEY, event);
    }
}
