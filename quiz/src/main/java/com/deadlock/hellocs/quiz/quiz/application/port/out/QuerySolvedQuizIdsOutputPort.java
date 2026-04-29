package com.deadlock.hellocs.quiz.quiz.application.port.out;

import java.util.Set;

public interface QuerySolvedQuizIdsOutputPort {
    Set<Long> findByUserId(Long userId);
}
