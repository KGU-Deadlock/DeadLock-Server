package com.deadlock.hellocs.quiz.grading.application.strategy;

import com.deadlock.hellocs.quiz.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.AiFeedback;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 단답형, 음성형 채점 전략.
 * AI를 통한 유연한 채점 (부분 점수 가능, 정답 임계값 70점).
 */
@Component
@RequiredArgsConstructor
public class AiGradingStrategy implements GradingStrategy {

    private static final int CORRECT_THRESHOLD = 70;

    private final CommandAiGradingOutputPort commandAiGradingPort;

    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.SHORT_ANSWER || type == QuizType.VOICE;
    }

    /** AI 피드백 점수를 0~100으로 정규화하고, {@value CORRECT_THRESHOLD}점 이상이면 정답으로 처리함. */
    @Override
    public GradingItem grade(GradingTarget target, String userAnswer) {
        AiFeedback feedback = commandAiGradingPort.evaluate(target, userAnswer);
        int normalizedScore = Math.max(0, Math.min(100, feedback.score()));

        return GradingItem.builder()
                .quizId(target.id())
                .quizContent(target.content())
                .quizType(target.type())
                .correctAnswer(target.correctAnswer())
                .score(normalizedScore)
                .isCorrect(normalizedScore >= CORRECT_THRESHOLD)
                .userAnswer(userAnswer)
                .feedback(feedback.message())
                .missingKeywords(feedback.missingKeywords() == null ? List.of() : feedback.missingKeywords())
                .improvedAnswer(feedback.improvedAnswer())
                .build();
    }
}
