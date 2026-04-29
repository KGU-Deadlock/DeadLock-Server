package com.deadlock.hellocs.quiz.grading.application.strategy;

import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OX, 객관식 채점 전략.
 * 정답/오답만 판단 (100점 or 0점).
 */
@Component
public class StandardGradingStrategy implements GradingStrategy {

    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.OX || type == QuizType.MULTIPLE_CHOICE;
    }

    /** 대소문자 구분 없이 정답과 비교하여 100점 또는 0점을 부여함. */
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
