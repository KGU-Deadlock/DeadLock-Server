package com.deadlock.hellocs.quiz.grading.domain;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 한 번의 채점 세션 결과를 담는 도메인 엔티티.
 * <p>채점 흐름: {@link GradingCommandService#submit} → {@code GradingLog.create()} → MongoDB 저장됨.</p>
 */
@Getter
@Builder
public class GradingLog {
    private String id;
    private Long userId;
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;
    private List<String> topicNames;

    /** 세션 내 모든 문제가 VOICE일 때만 VOICE 모드로 판단함. */
    public QuizMode getQuizMode() {
        boolean allVoice = results.stream().allMatch(i -> i.quizType() == QuizType.VOICE);
        return allVoice ? QuizMode.VOICE : QuizMode.STANDARD;
    }

    /** 정답 수를 자동 집계하여 채점 로그를 생성하는 팩토리 메서드. */
    public static GradingLog create(Long userId, List<GradingItem> results, List<String> topicNames) {
        int correctCount = (int) results.stream()
                .filter(GradingItem::isCorrect)
                .count();

        return GradingLog.builder()
                .userId(userId)
                .solvedAt(LocalDateTime.now())
                .totalCount(results.size())
                .correctCount(correctCount)
                .results(results)
                .topicNames(topicNames)
                .build();
    }
}
