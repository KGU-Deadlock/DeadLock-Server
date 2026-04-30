package com.deadlock.hellocs.interview.question.application.port.out;

import com.deadlock.hellocs.interview.question.domain.CsQuestion;

import java.util.List;

public interface QueryCsQuestionOutputPort {
    List<CsQuestion> findRandomTwoWithoutDuplicate();
}
