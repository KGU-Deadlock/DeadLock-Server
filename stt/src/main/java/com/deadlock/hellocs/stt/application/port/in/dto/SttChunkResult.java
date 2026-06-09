package com.deadlock.hellocs.stt.application.port.in.dto;

public record SttChunkResult(
        String sessionId,
        Long sequence,
        String text,
        boolean isFinalChunk
) {
}
