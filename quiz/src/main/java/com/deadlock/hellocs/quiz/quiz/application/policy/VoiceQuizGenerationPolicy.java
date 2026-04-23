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
 * 음성 Quiz 생성 정책
 * 
 * Voice 3개
 */
@Component
@RequiredArgsConstructor
public class VoiceQuizGenerationPolicy implements QuizGenerationPolicy {

    private static final Map<QuizType, Integer> COMPOSITION = Map.of(
            QuizType.VOICE, 3
    );

    private final QueryGradingLogOutputPort queryGradingLogPort;

    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.VOICE;
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
