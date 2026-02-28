package com.deadlock.hellocs.quiz.stt.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.deadlock.hellocs.quiz.stt.application.port.in.dto.SttChunkCommand;
import com.deadlock.hellocs.quiz.stt.application.port.out.CommandSttAiOutputPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiSttAdapter implements CommandSttAiOutputPort {

    @Value("${ai.stt.transcribe-endpoint}")
    private String transcribeEndpoint;

    private final RestClient.Builder restClientBuilder;

    @Override
    public String transcribeChunk(SttChunkCommand command) {
        byte[] audioBytes = decodeAudio(command.audioBase64());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(audioBytes, "audio.wav"));
        body.add("language", "ko");
        body.add("task", "transcribe");

        try {
            SttTranscribeResponse response = restClientBuilder.build()
                    .post()
                    .uri(transcribeEndpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SttTranscribeResponse.class);

            return response == null || response.text() == null ? "" : response.text();
        } catch (RuntimeException e) {
            log.error("STT transcribe request failed. endpoint={}", transcribeEndpoint, e);
            throw e;
        }
    }

    private byte[] decodeAudio(String audioBase64) {
        String normalized = audioBase64;
        int prefixSeparator = audioBase64.indexOf(',');
        if (audioBase64.startsWith("data:") && prefixSeparator >= 0) {
            normalized = audioBase64.substring(prefixSeparator + 1);
        }
        return Base64.getDecoder().decode(normalized);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SttTranscribeResponse(String text) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
