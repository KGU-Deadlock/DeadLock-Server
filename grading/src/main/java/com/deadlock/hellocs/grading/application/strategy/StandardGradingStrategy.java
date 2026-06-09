package com.deadlock.hellocs.grading.application.strategy;

import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.contract.QuizType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OX, 객관식 채점 전략.
 */
@Component
public class StandardGradingStrategy implements GradingStrategy {

    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.OX || type == QuizType.MULTIPLE_CHOICE;
    }

    @Override
    public GradingItem grade(GradingTarget target, String userAnswer) {
        boolean isCorrect = target.correctAnswer().equalsIgnoreCase(userAnswer);

        return GradingItem.builder()
                .quizId(target.id())
                .quizContent(target.content())
                .quizType(target.type())
                .correctAnswer(target.correctAnswer())
                .score(isCorrect ? 100 : 0)
                .isCorrect(isCorrect)
                .userAnswer(userAnswer)
                .feedback(target.explanation())
                .missingKeywords(List.of())
                .improvedAnswer(null)
                .build();
    }
}
