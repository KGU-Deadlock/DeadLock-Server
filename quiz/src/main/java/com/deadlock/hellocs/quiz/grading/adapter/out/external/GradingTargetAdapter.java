package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingTargetOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryTopicOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingSessionView;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.QuizSession;
import com.deadlock.hellocs.quiz.quiz.domain.QuizSessionEntry;
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
    private final QueryTopicOutputPort queryTopicPort;

    @Override
    public GradingSessionView fetchSession(Long userId, List<Long> quizIds) {
        QuizSession session = queryQuizSessionPort.findByUserId(userId);

        Map<Long, GradingTarget> targets = quizIds.stream()
                .map(id -> session.quizzes().get(id))
                .filter(Objects::nonNull)
                .map(this::toGradingTarget)
                .collect(Collectors.toMap(GradingTarget::id, Function.identity()));

        List<String> topicNames = queryTopicPort.getTopicNames(session.topicIds());

        return new GradingSessionView(session.mode(), session.topicIds(), topicNames, targets);
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
