package com.deadlock.hellocs.streak.adapter.in.event;

import com.deadlock.hellocs.streak.application.port.in.RecordStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RabbitMQ 메시지를 수신하여 스트릭을 갱신하는 인바운드 어댑터.
 * <p>
 * 채점 완료({@code streak.grading.completed})와 인터뷰 완료({@code streak.interview.completed})
 * 두 큐를 각각 처리합니다.
 * 예외 발생 시 ACK 후 로그만 남겨 메시지 무한 재처리를 방지합니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakEventListener {

    private final RecordStreakUseCase recordStreakUseCase;

    @RabbitListener(queues = "streak.grading.completed")
    public void handleGradingCompleted(GradingCompletedMessage message) {
        if (message.solvedAt() == null) {
            return;
        }
        try {
            recordStreakUseCase.record(new RecordStreakCommand(
                    message.gradingLogId(),
                    message.userId(),
                    message.solvedAt().toLocalDate(),
                    message.quizCount(),
                    message.topicIds()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak from grading. gradingLogId={}", message.gradingLogId(), e);
        }
    }

    @RabbitListener(queues = "streak.interview.completed")
    public void handleInterviewCompleted(InterviewCompletedMessage message) {
        try {
            recordStreakUseCase.record(new RecordStreakCommand(
                    message.interviewId(),
                    message.userId(),
                    message.completedAt().toLocalDate(),
                    message.questionCount(),
                    List.of()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak from interview. interviewId={}", message.interviewId(), e);
        }
    }
}
