package com.deadlock.hellocs.interview.feedback.adapter.out.external;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.interview.exception.InterviewErrorStatus;
import com.deadlock.hellocs.interview.feedback.application.port.out.AiFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackRequest;
import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

@Component
@Slf4j
public class AiFeedbackAdapter implements AiFeedbackOutputPort {

    private final RestClient restClient;
    private final String feedbackEndpoint;

    public AiFeedbackAdapter(
            @Value("${ai.interview.feedback-endpoint}") String feedbackEndpoint,
            @Value("${ai.interview.connect-timeout:2000}") long connectTimeoutMs,
            @Value("${ai.interview.read-timeout:30000}") long readTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.feedbackEndpoint = feedbackEndpoint;
    }

    @Override
    public AiFeedbackResponse evaluate(AiFeedbackRequest request) {
        try {
            AiFeedbackResponse response = restClient.post()
                    .uri(feedbackEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiFeedbackResponse.class);

            Objects.requireNonNull(response);
            return response;

        } catch (RestClientException | NullPointerException e) {
            log.error("AI 피드백 요청 실패. endpoint={}", feedbackEndpoint, e);
            throw new CustomException(InterviewErrorStatus.INTERVIEW_AI_FEEDBACK_FAILED);
        }
    }
}
