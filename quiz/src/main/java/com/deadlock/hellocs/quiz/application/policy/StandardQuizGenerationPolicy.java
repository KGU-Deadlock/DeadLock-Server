package com.deadlock.hellocs.quiz.application.policy;

import com.deadlock.hellocs.quiz.application.port.out.QuerySolvedQuizIdsOutputPort;
import com.deadlock.hellocs.quiz.contract.QuizMode;
import com.deadlock.hellocs.quiz.contract.QuizType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StandardQuizGenerationPolicy extends AbstractQuizGenerationPolicy {

    private static final Map<QuizType, Integer> COMPOSITION = Map.of(
            QuizType.OX, 2,
            QuizType.MULTIPLE_CHOICE, 2,
            QuizType.SHORT_ANSWER, 1
    );

    public StandardQuizGenerationPolicy(QuerySolvedQuizIdsOutputPort querySolvedQuizIdsPort) {
        super(querySolvedQuizIdsPort);
    }

    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.STANDARD;
    }

    @Override
    protected Map<QuizType, Integer> getComposition() {
        return COMPOSITION;
    }
}
