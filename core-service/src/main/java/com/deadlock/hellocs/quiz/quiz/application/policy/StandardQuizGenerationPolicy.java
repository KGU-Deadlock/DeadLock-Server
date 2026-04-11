package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 표준 Quiz 생성 정책
 * 
 * OX 2개 + 객관식 2개 + 단답형 1개 = 총 5개
 */
@Component
@RequiredArgsConstructor
public class StandardQuizGenerationPolicy implements QuizGenerationPolicy {

    private static final Map<QuizType, Integer> COMPOSITION = Map.of(
            QuizType.OX, 2,
            QuizType.MULTIPLE_CHOICE, 2,
            QuizType.SHORT_ANSWER, 1
    );

    private final QueryGradingLogOutputPort queryGradingLogPort;

    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.STANDARD;
    }

    @Override
    public List<Quiz> generate(GetQuizCommand command, QueryQuizOutputPort queryQuizPort, Long userId) {
        Set<Long> solvedQuizIds = queryGradingLogPort.findAllByUserId(userId).stream()
                .flatMap(log -> log.getResults().stream())
                .map(GradingItem::quizId)
                .collect(Collectors.toSet());

        List<Quiz> quizzes = new ArrayList<>();

        COMPOSITION.forEach((type, count) -> {
            List<Quiz> candidates = queryQuizPort.findQuizzesByCriteria(
                    command.level(),
                    command.topicIds(),
                    type
            );
            List<Quiz> filtered = candidates.stream()
                    .filter(q -> !solvedQuizIds.contains(q.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(filtered);
            quizzes.addAll(filtered.stream().limit(count).toList());
        });

        return quizzes;
    }
}
