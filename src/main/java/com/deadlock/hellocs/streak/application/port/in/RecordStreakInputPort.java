package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;

public interface RecordStreakInputPort {
    void record(RecordStreakCommand command);
}
