package com.deadlock.hellocs.quiz.stt.adapter.out.external;

import com.deadlock.hellocs.quiz.stt.application.port.in.dto.SttChunkCommand;
import com.deadlock.hellocs.quiz.stt.application.port.out.CommandSttAiOutputPort;
import org.springframework.stereotype.Component;

@Component
public class AiSttAdapter implements CommandSttAiOutputPort {

    @Override
    public String transcribeChunk(SttChunkCommand command) {
        return "asdsada";
    }
}
