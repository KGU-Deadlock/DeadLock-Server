package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QuerySolvedQuizIdsOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SolvedQuizIdsAdapter implements QuerySolvedQuizIdsOutputPort {

    private final QueryGradingLogOutputPort queryGradingLogPort;

    @Override
    public Set<Long> findByUserId(Long userId) {
        return queryGradingLogPort.findAllByUserId(userId).stream()
                .flatMap(log -> log.getResults().stream())
                .map(GradingItem::quizId)
                .collect(Collectors.toSet());
    }
}
