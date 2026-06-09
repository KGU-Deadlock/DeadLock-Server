package com.deadlock.hellocs.grading.adapter.out.external;

import com.deadlock.hellocs.grading.application.port.out.QueryGradingTargetOutputPort;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingSessionView;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.contract.QuizSession;
import com.deadlock.hellocs.quiz.contract.QuizSessionEntry;
import com.deadlock.hellocs.quiz.contract.QueryQuizSessionOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GradingTargetAdapter implements QueryGradingTargetOutputPort {

    private final QueryQuizSessionOutputPort queryQuizSessionPort;

    @Override
    public GradingSessionView fetchSession(Long userId, List<Long> quizIds) {
        QuizSession session = queryQuizSessionPort.findByUserIdAndQuizIds(userId, quizIds);

        Map<Long, GradingTarget> targets = quizIds.stream()
                .map(id -> session.quizzes().get(id))
                .filter(Objects::nonNull)
                .map(this::toGradingTarget)
                .collect(Collectors.toMap(GradingTarget::id, Function.identity()));

        return new GradingSessionView(session.mode(), session.topicIds(), targets);
    }

    private GradingTarget toGradingTarget(QuizSessionEntry entry) {
        return new GradingTarget(
                entry.quizId(),
                entry.type(),
                entry.content(),
                entry.correctAnswer(),
                entry.explanation()
        );
    }
}
