package com.deadlock.hellocs.grading.application.port.in;

import java.util.Set;

public interface QuerySolvedQuizIdsInputPort {
    Set<Long> getSolvedQuizIds(Long userId);
}
