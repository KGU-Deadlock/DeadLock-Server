package com.deadlock.hellocs.grading.application.service;

import com.deadlock.hellocs.grading.application.port.in.QuerySolvedQuizIdsInputPort;
import com.deadlock.hellocs.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.grading.domain.GradingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolvedQuizIdsService implements QuerySolvedQuizIdsInputPort {

    private final QueryGradingLogOutputPort queryGradingLogPort;

    @Override
    public Set<Long> getSolvedQuizIds(Long userId) {
        return queryGradingLogPort.findAllByUserId(userId).stream()
                .flatMap(log -> log.getResults().stream())
                .map(GradingItem::quizId)
                .collect(Collectors.toSet());
    }
}
