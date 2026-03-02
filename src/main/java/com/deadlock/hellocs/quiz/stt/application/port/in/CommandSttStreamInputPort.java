package com.deadlock.hellocs.quiz.stt.application.port.in;

import com.deadlock.hellocs.quiz.stt.application.port.in.dto.SttChunkCommand;
import com.deadlock.hellocs.quiz.stt.application.port.in.dto.SttChunkResult;

public interface CommandSttStreamInputPort {
    SttChunkResult process(SttChunkCommand command);
}
