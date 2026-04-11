package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import jakarta.validation.Valid;

public interface RecordStreakInputPort {
    void record(@Valid RecordStreakCommand command);
}
