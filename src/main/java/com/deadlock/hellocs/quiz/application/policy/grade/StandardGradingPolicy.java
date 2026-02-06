package com.deadlock.hellocs.quiz.application.policy.grade;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.domain.Quiz;
import org.springframework.stereotype.Component;

@Component
public class StandardGradingPolicy implements GradingPolicy {

    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.OX || type == QuizType.MULTIPLE_CHOICE;
    }

    @Override
    public GradingResult grade(Quiz quiz, String userAnswer) {
        boolean isCorrect = quiz.isMatch(userAnswer);

        return GradingResult.builder()
                .quizId(quiz.getId())
                .correctAnswer(quiz.getCorrectAnswerStr())
                .feedback(quiz.getExplain())
                .score(isCorrect ? 100 : 0)
                .build();
    }
}
