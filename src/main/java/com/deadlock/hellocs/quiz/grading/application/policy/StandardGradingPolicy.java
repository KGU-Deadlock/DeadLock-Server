package com.deadlock.hellocs.quiz.grading.application.policy;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.quiz.domain.vo.QuizAnswer;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 표준 채점 정책
 * 
 * OX, 객관식에 사용
 * 정답/오답만 판단 (100점 or 0점)
 */
@Component
@RequiredArgsConstructor
public class StandardGradingPolicy implements GradingPolicy {
    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.OX || type == QuizType.MULTIPLE_CHOICE;
    }
    
    @Override
    public GradingItem grade(Quiz quiz, String userAnswer) {
        QuizAnswer quizAnswer = quiz.getAnswer();
        boolean isCorrect = quizAnswer.asString().equalsIgnoreCase(userAnswer);
        
        return GradingItem.builder()
                .quizId(quiz.getId())
                .score(isCorrect ? 100 : 0)
                .isCorrect(isCorrect)
                .userAnswer(userAnswer)
                .feedback(quiz.getExplain())
                .build();
    }
}
