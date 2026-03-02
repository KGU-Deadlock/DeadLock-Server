package com.deadlock.hellocs.streak.adapter.in.event;

import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.streak.application.port.in.RecordStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakGradingCompletedEventListener {

    private final RecordStreakInputPort recordStreakInputPort;
    // 수정 표시
    private final QueryGradingLogOutputPort queryGradingLogOutputPort;
    // 수정 표시
    private final QueryQuizOutputPort queryQuizOutputPort;

    @EventListener
    public void handle(GradingCompletedEvent event) {
        try {
            // 수정 표시
            if (event == null || event.gradingLogId() == null || event.solvedAt() == null || event.userId() == null) {
                return;
            }

            var gradingLog = queryGradingLogOutputPort.findById(event.gradingLogId());
            if (gradingLog == null || gradingLog.getResults() == null || gradingLog.getResults().isEmpty()) {
                return;
            }

            var quizIds = gradingLog.getResults().stream()
                    .map(result -> result.getQuizId())
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (quizIds.isEmpty()) {
                return;
            }

            var quizzes = queryQuizOutputPort.findAllByIds(quizIds);
            if (quizzes == null || quizzes.isEmpty()) {
                return;
            }

            var topicIds = quizzes.stream()
                    .filter(quiz -> quiz != null && quiz.getTopicIds() != null)
                    .flatMap(quiz -> quiz.getTopicIds().stream())
                    .filter(id -> id != null)
                    .distinct()
                    .toList();

            recordStreakInputPort.record(new RecordStreakCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.solvedAt().toLocalDate(),
                    event.quizCount()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak. gradingLogId={}", event.gradingLogId(), e);
        }
    }
}
