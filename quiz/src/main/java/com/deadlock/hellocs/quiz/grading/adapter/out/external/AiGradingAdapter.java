package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.AiFeedback;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
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
 * <p>설정값({@code ai.grading.*})으로 엔드포인트·타임아웃을 주입받아 {@link RestClient}로 호출함.</p>
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

    /** 퀴즈·정답·사용자 답변을 AI 서버에 전달하고 피드백을 받음. */
    @Override
    public AiFeedback evaluate(GradingTarget target, String userAnswer) {
        FeedbackRequest request = new FeedbackRequest(target.content(), userAnswer, target.correctAnswer());
        return callEvaluateApi(request);
    }

    /** AI 서버 호출 실패 또는 응답 파싱 오류 시 {@code GRADING_AI_EVALUATION_FAILED} 예외를 던짐. */
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
            throw new CustomException(QuizErrorStatus.GRADING_AI_EVALUATION_FAILED);
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
