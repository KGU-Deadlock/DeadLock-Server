package com.deadlock.hellocs.grading.application.strategy;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.quiz.contract.QuizType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link QuizType} → {@link GradingStrategy} 매핑 레지스트리.
 */
@Component
@RequiredArgsConstructor
public class GradingStrategyRegistry {

    private final List<GradingStrategy> strategies;
    private Map<QuizType, GradingStrategy> strategyMap;

    @PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .flatMap(s -> java.util.Arrays.stream(QuizType.values())
                        .filter(s::supports)
                        .map(type -> Map.entry(type, s)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public GradingStrategy resolve(QuizType type) {
        GradingStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new CustomException(GradingErrorStatus.GRADING_POLICY_NOT_FOUND);
        }
        return strategy;
    }
}
