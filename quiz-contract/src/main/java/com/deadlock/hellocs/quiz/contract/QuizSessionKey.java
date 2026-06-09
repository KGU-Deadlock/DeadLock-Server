package com.deadlock.hellocs.quiz.contract;

import java.util.List;
import java.util.stream.Collectors;

/**
 * quiz-service(쓰기) · grading-service(읽기) 가 공유 Redis 에서
 * QuizSession 을 동일한 키로 접근할 수 있도록 키 생성 규칙을 공통화.
 *
 * 키 형식: quiz:session:{userId}:{정렬된 quizId 목록, 쉼표 구분}
 */
public final class QuizSessionKey {

    private static final String KEY_PREFIX = "quiz:session:";

    private QuizSessionKey() {}

    public static String of(Long userId, List<Long> quizIds) {
        String ids = quizIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return KEY_PREFIX + userId + ":" + ids;
    }
}
