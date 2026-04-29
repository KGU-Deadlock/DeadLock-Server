package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingTargetOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingSessionView;
import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.grading.application.strategy.GradingStrategyRegistry;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * 채점 오케스트레이터. {@link QuizGradingController}에서 호출됨.
 * <p>흐름: 세션 조회 → {@link GradingStrategyRegistry}로 전략 선택 → 채점 → {@link GradingLog} 저장 → {@link GradingCompletedEvent} 발행</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class GradingCommandService implements CommandAnswerInputPort {

    private final QueryGradingTargetOutputPort queryGradingTargetPort;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final GradingStrategyRegistry gradingStrategyRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 세션 조회 → 전략별 채점 → 로그 저장 → 이벤트 발행 순으로 처리하며, 채점 로그 ID를 반환함. */
    @Override
    public String submit(SubmitAnswersCommand command) {
        List<UserGradingCommand> answers = command.answers();

        List<Long> quizIds = extractQuizIds(answers);

        GradingSessionView session = queryGradingTargetPort.fetchSession(command.userId(), quizIds);

        List<GradingItem> gradingItems = gradeAnswers(answers, session.targets());

        GradingLog gradingLog = GradingLog.create(command.userId(), session.mode(), gradingItems, session.topicNames());
        GradingLog savedGradingLog = commandGradingLogPort.save(gradingLog);

        applicationEventPublisher.publishEvent(new GradingCompletedEvent(
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
                    if (target == null) throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
                    return gradeAnswer(target, answer.answer());
                })
                .toList();
    }

    private GradingItem gradeAnswer(GradingTarget target, String answer) {
        return gradingStrategyRegistry.resolve(target.type()).grade(target, answer);
    }
}
