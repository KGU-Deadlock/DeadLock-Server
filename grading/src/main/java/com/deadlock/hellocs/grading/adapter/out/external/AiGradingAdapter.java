package com.deadlock.hellocs.grading.adapter.out.external;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.grading.application.port.out.dto.AiFeedback;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 외부 AI 채점 서버와 통신하는 어댑터.
 */
@Component
@Slf4j
public class AiGradingAdapter implements CommandAiGradingOutputPort {

    private final RestClient restClient;
    private final String evaluateEndpoint;

    public AiGradingAdapter(
            @Value("${ai.grading.evaluate-endpoint}") String evaluateEndpoint,
            @Value("${ai.grading.connect-timeout:2000}") long connectTimeoutMs,
            @Value("${ai.grading.read-timeout:10000}") long readTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.evaluateEndpoint = evaluateEndpoint;
    }

    @Override
    public AiFeedback evaluate(GradingTarget target, String userAnswer) {
        FeedbackRequest request = new FeedbackRequest(target.content(), userAnswer, target.correctAnswer());
        return callEvaluateApi(request);
    }

    private AiFeedback callEvaluateApi(FeedbackRequest request) {
        try {
            FeedbackResponse response = restClient.post()
                    .uri(evaluateEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FeedbackResponse.class);

            Objects.requireNonNull(response);
            return new AiFeedback(response.score(), response.message(), response.missingKeywords(), response.improvedAnswer());

        } catch (RestClientException | NullPointerException e) {
            log.error("AI grading request failed. endpoint={}", evaluateEndpoint, e);
            throw new CustomException(GradingErrorStatus.GRADING_AI_EVALUATION_FAILED);
        }
    }

    private record FeedbackRequest(
            @JsonProperty("question") String question,
            @JsonProperty("user_answer") String userAnswer,
            @JsonProperty("model_answer") String modelAnswer
    ) {
    }

    private record FeedbackResponse(
            @JsonProperty("score") int score,
            @JsonProperty("missing_keywords") List<String> missingKeywords,
            @JsonProperty("improved_answer") String improvedAnswer,
            @JsonProperty("message") String message
    ) {
    }
}
