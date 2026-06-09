package com.deadlock.hellocs.stt.adapter.in.websocket;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.stt.application.port.in.CommandSttStreamInputPort;
import com.deadlock.hellocs.stt.application.port.in.dto.SttChunkCommand;
import com.deadlock.hellocs.stt.application.port.in.dto.SttChunkResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SttWebSocketController {

    private static final String STT_SUBSCRIBE_PREFIX = "/v1/ws/sub/stt/";

    private final CommandSttStreamInputPort commandSttStreamInputPort;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/stt/chunk")
    public void handleChunk(@Valid SttChunkCommand command) {
        SttChunkResult result = commandSttStreamInputPort.process(command);
        messagingTemplate.convertAndSend(
                STT_SUBSCRIBE_PREFIX + result.sessionId(),
                result
        );
    }
}
