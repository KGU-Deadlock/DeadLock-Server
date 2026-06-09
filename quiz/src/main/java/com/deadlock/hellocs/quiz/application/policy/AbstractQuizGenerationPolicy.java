package com.deadlock.hellocs.quiz.application.policy;

import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.application.port.out.QuerySolvedQuizIdsOutputPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizType;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractQuizGenerationPolicy implements QuizGenerationPolicy {

    protected final QuerySolvedQuizIdsOutputPort querySolvedQuizIdsPort;

    protected abstract Map<QuizType, Integer> getComposition();

    @Override
    public List<Quiz> generate(GetQuizCommand command, QueryQuizOutputPort queryQuizPort, Long userId, QuizLevel level) {
        Set<Long> solvedQuizIds = querySolvedQuizIdsPort.findByUserId(userId);
        List<Quiz> quizzes = new ArrayList<>();
        getComposition().forEach((type, count) -> {
            List<Quiz> candidates = queryQuizPort.findQuizzesByCriteria(level, command.topicIds(), type);
            List<Quiz> filtered = candidates.stream()
                    .filter(q -> !solvedQuizIds.contains(q.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(filtered);
            quizzes.addAll(filtered.stream().limit(count).toList());
        });
        return quizzes;
    }
}
