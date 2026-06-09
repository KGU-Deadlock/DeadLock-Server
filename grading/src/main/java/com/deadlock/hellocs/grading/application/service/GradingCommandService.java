package com.deadlock.hellocs.grading.application.service;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.grading.adapter.out.event.GradingCompletedEvent;
import com.deadlock.hellocs.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.UserGradingCommand;
import com.deadlock.hellocs.grading.application.port.out.CommandGradingEventOutputPort;
import com.deadlock.hellocs.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.grading.application.port.out.QueryGradingTargetOutputPort;
import com.deadlock.hellocs.grading.application.port.out.QueryTopicOutputPort;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingSessionView;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.grading.application.strategy.GradingStrategyRegistry;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * 채점 오케스트레이터.
 * 흐름: 세션 조회 → 전략 선택 → 채점 → GradingLog 저장 → 이벤트 발행
 */
@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class GradingCommandService implements CommandAnswerInputPort {

    private final QueryGradingTargetOutputPort queryGradingTargetPort;
    private final QueryTopicOutputPort queryTopicPort;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final CommandGradingEventOutputPort commandGradingEventPort;
    private final GradingStrategyRegistry gradingStrategyRegistry;

    @Override
    public String submit(SubmitAnswersCommand command) {
        List<Long> quizIds = extractQuizIds(command.answers());

        GradingSessionView session = queryGradingTargetPort.fetchSession(command.userId(), quizIds);

        List<GradingItem> gradingItems = gradeAnswers(command.answers(), session.targets());

        List<String> topicNames = queryTopicPort.getTopicNames(session.topicIds());
        GradingLog gradingLog = GradingLog.create(command.userId(), session.mode(), gradingItems, topicNames);
        GradingLog savedGradingLog = commandGradingLogPort.save(gradingLog);

        commandGradingEventPort.publish(new GradingCompletedEvent(
                savedGradingLog.getId(),
                savedGradingLog.getUserId(),
                savedGradingLog.getSolvedAt(),
                savedGradingLog.getTotalCount(),
                savedGradingLog.getResults().stream().mapToInt(GradingItem::score).sum(),
                session.topicIds()
        ));

        return savedGradingLog.getId();
    }

    private List<Long> extractQuizIds(List<UserGradingCommand> answers) {
        return answers.stream()
                .map(UserGradingCommand::quizId)
                .toList();
    }

    private List<GradingItem> gradeAnswers(List<UserGradingCommand> answers, Map<Long, GradingTarget> targets) {
        return answers.stream()
                .map(answer -> {
                    GradingTarget target = targets.get(answer.quizId());
                    if (target == null) throw new CustomException(GradingErrorStatus.GRADING_QUIZ_NOT_FOUND);
                    return gradeAnswer(target, answer.answer());
                })
                .toList();
    }

    private GradingItem gradeAnswer(GradingTarget target, String answer) {
        return gradingStrategyRegistry.resolve(target.type()).grade(target, answer);
    }
}
