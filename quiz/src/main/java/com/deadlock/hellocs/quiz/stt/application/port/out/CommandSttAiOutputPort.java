package com.deadlock.hellocs.quiz.stt.application.port.out;

import com.deadlock.hellocs.quiz.stt.application.port.in.dto.SttChunkCommand;

public interface CommandSttAiOutputPort {
    String transcribeChunk(SttChunkCommand command);
}
