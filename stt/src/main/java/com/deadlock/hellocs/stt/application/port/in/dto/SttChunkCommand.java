package com.deadlock.hellocs.stt.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SttChunkCommand(
        @NotBlank String sessionId,
        @NotNull @PositiveOrZero Long sequence,
        @NotBlank String audioBase64,
        boolean isFinalChunk
) {
}
