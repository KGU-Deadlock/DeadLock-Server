package com.deadlock.hellocs.stt.application.port.out;

import com.deadlock.hellocs.stt.application.port.in.dto.SttChunkCommand;

public interface CommandSttAiOutputPort {
    String transcribeChunk(SttChunkCommand command);
}
