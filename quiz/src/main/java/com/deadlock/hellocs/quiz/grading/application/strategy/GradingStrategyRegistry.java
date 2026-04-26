package com.deadlock.hellocs.quiz.grading.application.strategy;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link QuizType} → {@link GradingStrategy} 매핑 레지스트리.
 * <p>애플리케이션 시작 시 {@code @PostConstruct}로 전략 맵을 초기화하여, 채점 시 O(1)로 전략을 조회함.</p>
 */
@Component
@RequiredArgsConstructor
public class GradingStrategyRegistry {

    private final List<GradingStrategy> strategies;
    private Map<QuizType, GradingStrategy> strategyMap;

    /** 등록된 전략을 {@link QuizType}별로 인덱싱하여 채점 시 O(1) 조회를 보장함. */
    @PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .flatMap(s -> java.util.Arrays.stream(QuizType.values())
                        .filter(s::supports)
                        .map(type -> Map.entry(type, s)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** 지원하는 전략이 없으면 {@code GRADING_POLICY_NOT_FOUND} 예외를 던짐. */
    public GradingStrategy resolve(QuizType type) {
        GradingStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new CustomException(QuizErrorStatus.GRADING_POLICY_NOT_FOUND);
        }
        return strategy;
    }
}
