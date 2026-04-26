package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingTargetOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryTopicOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingSessionView;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quiz 모듈 의존을 Grading 모듈 경계로 격리하는 어댑터.
 * QuizSession 도입 전까지 quiz 모듈로부터 세션 뷰를 합성한다.
 * QuizSession이 도입되면 이 어댑터만 교체하여 grading core 변경 없이 마이그레이션 가능.
 */
@Component
@RequiredArgsConstructor
public class GradingTargetAdapter implements QueryGradingTargetOutputPort {

    private final QueryQuizOutputPort queryQuizPort;
    private final QueryTopicOutputPort queryTopicPort;

    /** quiz 모듈에서 퀴즈·토픽 정보를 조합하여 {@link GradingSessionView}를 반환함. */
    @Override
    public GradingSessionView fetchSession(Long userId, List<Long> quizIds) {
        // QuizSession 도입 시: userId로 세션 소유자 검증 위치
        List<Quiz> quizzes = queryQuizPort.findAllByIds(quizIds);

        Map<Long, GradingTarget> targets = quizzes.stream()
                .map(this::toGradingTarget)
                .collect(Collectors.toMap(GradingTarget::id, Function.identity()));

        List<Long> topicIds = quizzes.stream()
                .flatMap(q -> q.getTopicIds().stream())
                .distinct()
                .toList();

        List<String> topicNames = queryTopicPort.getTopicNames(topicIds);

        return new GradingSessionView(topicIds, topicNames, targets);
    }

    private GradingTarget toGradingTarget(Quiz quiz) {
        return new GradingTarget(
                quiz.getId(),
                quiz.getType(),
                quiz.getContent(),
                quiz.getAnswer().asString(),
                quiz.getExplain()
        );
    }
}
