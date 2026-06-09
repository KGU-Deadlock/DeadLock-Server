package com.deadlock.hellocs.stt.application.service;

import com.deadlock.hellocs.stt.application.port.in.CommandSttStreamInputPort;
import com.deadlock.hellocs.stt.application.port.in.dto.SttChunkCommand;
import com.deadlock.hellocs.stt.application.port.in.dto.SttChunkResult;
import com.deadlock.hellocs.stt.application.port.out.CommandSttAiOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SttStreamService implements CommandSttStreamInputPort {

    private final CommandSttAiOutputPort commandSttAiOutputPort;

    @Override
    public SttChunkResult process(SttChunkCommand command) {
        String text = commandSttAiOutputPort.transcribeChunk(command);

        return new SttChunkResult(
                command.sessionId(),
                command.sequence(),
                text,
                command.isFinalChunk()
        );
    }
}
