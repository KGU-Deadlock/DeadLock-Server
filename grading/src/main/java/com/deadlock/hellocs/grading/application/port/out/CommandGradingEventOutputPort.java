package com.deadlock.hellocs.grading.application.port.out;

import com.deadlock.hellocs.grading.adapter.out.event.GradingCompletedEvent;

public interface CommandGradingEventOutputPort {
    void publish(GradingCompletedEvent event);
}
