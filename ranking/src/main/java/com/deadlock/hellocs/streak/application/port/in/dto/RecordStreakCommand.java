package com.deadlock.hellocs.streak.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record RecordStreakCommand(
        @NotBlank String gradingLogId,
        @NotNull Long userId,
        @NotNull LocalDate solvedDate,
        @Positive int quizCount,
        @NotNull List<Long> topicIds
) {}
