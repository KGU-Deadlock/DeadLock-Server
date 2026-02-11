package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 표준 Quiz 생성 정책
 * 
 * OX 2개 + 객관식 2개 + 단답형 1개 = 총 5개
 */
@Component
public class StandardQuizGenerationPolicy implements QuizGenerationPolicy {

    private static final Map<QuizType, Integer> COMPOSITION = Map.of(
            QuizType.OX, 2,
            QuizType.MULTIPLE_CHOICE, 2,
            QuizType.SHORT_ANSWER, 1
    );
    
    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.STANDARD;
    }
    
    @Override
    public List<Quiz> generate(GetQuizCommand command, QueryQuizOutputPort queryQuizPort) {
        List<Quiz> quizzes = new ArrayList<>();

        COMPOSITION.forEach((type, count) -> {
            List<Quiz> foundQuizzes = queryQuizPort.findQuizzesByCriteria(
                    command.level(),
                    command.topicIds(),
                    type,
                    count
            );
            quizzes.addAll(foundQuizzes);
        });

        return quizzes;
    }
}
