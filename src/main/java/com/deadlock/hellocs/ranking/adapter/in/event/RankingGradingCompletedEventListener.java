package com.deadlock.hellocs.ranking.adapter.in.event;

import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.ranking.application.port.in.ApplyRankingScoreInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingGradingCompletedEventListener {

    // 수정 표시
    private final ApplyRankingScoreInputPort applyRankingScoreInputPort;
    // 수정 표시
    private final QueryGradingLogOutputPort queryGradingLogOutputPort;
    // 수정 표시
    private final QueryQuizOutputPort queryQuizOutputPort;

    @EventListener
    public void handle(GradingCompletedEvent event) {
        try {
            // 수정 표시
            if (event == null || event.gradingLogId() == null || event.userId() == null) {
                return;
            }

            var gradingLog = queryGradingLogOutputPort.findById(event.gradingLogId());
            if (gradingLog == null || gradingLog.getResults() == null || gradingLog.getResults().isEmpty()) {
                return;
            }

            var quizIds = gradingLog.getResults().stream()
                    .map(result -> result.quizId())
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

            applyRankingScoreInputPort.apply(new ApplyRankingScoreCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.totalScore(),
                    topicIds
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update ranking. gradingLogId={}", event.gradingLogId(), e);
        }
    }
}
