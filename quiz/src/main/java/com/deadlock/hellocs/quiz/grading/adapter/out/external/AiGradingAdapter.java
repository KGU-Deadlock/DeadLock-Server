package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

/**
 * AI 채점 External Adapter
 *
 * 실제 AI 서비스 호출
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiGradingAdapter implements CommandAiGradingOutputPort {

    @Value("${ai.grading.evaluate-endpoint}")
    private String evaluateEndpoint;

    @Override
    public GradingItem gradeWithAi(Quiz quiz, String userAnswer) {
        FeedbackRequest request = new FeedbackRequest(
                quiz.getContent(),
                userAnswer,
                quiz.getAnswer().asString()
        );

        FeedbackResponse response = evaluate(request);
        int normalizedScore = normalizeScore(response.score());

        return GradingItem.builder()
                .quizId(quiz.getId())
                .score(normalizedScore)
                .isCorrect(normalizedScore >= 70)
                .userAnswer(userAnswer)
                .feedback(response.message())
                .missingKeywords(response.missingKeywords() == null ? List.of() : response.missingKeywords())
                .improvedAnswer(response.improvedAnswer())
                .build();
    }

    private FeedbackResponse evaluate(FeedbackRequest request) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            RestClient restClient = RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();

            FeedbackResponse response = restClient.post()
                    .uri(evaluateEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FeedbackResponse.class);

            if (response == null) {
                throw new CustomException(QuizErrorStatus.GRADING_AI_EVALUATION_FAILED);
            }
            return response;
        } catch (Exception e) {
            log.error("AI grading request failed. endpoint={}", evaluateEndpoint, e);
            throw new CustomException(QuizErrorStatus.GRADING_AI_EVALUATION_FAILED);
        }
    }

    private int normalizeScore(int score) {
        return Math.max(0, Math.min(100, score));
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
