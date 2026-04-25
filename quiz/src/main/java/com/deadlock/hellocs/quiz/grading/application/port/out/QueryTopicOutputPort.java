package com.deadlock.hellocs.quiz.grading.application.port.out;

import java.util.List;

public interface QueryTopicOutputPort {
    List<String> getTopicNames(List<Long> ids);
}
