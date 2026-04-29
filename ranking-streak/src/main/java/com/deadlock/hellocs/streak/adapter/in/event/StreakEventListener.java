package com.deadlock.hellocs.streak.adapter.in.event;

import com.deadlock.hellocs.streak.application.port.in.RecordStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 스트릭 갱신 이벤트를 수신하여 스트릭 기록을 처리하는 인바운드 어댑터.
 *
 * <p>이벤트 드리븐 구조로 quiz 모듈과 streak 모듈의 직접 의존을 끊어줌.
 * 런타임 예외 발생 시 스트릭 갱신을 건너뛰고 로그만 남겨 채점 흐름에 영향을 주지 않음.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakEventListener {

    private final RecordStreakUseCase recordStreakUseCase;

    /**
     * 수신한 이벤트를 {@link RecordStreakCommand}로 변환하여 유스케이스에 위임함.
     * 예외 발생 시 로그를 남기고 정상 종료하여 채점 트랜잭션 롤백을 방지함.
     */
    @EventListener
    public void handle(StreakUpdateEvent event) {
        try {
            recordStreakUseCase.record(new RecordStreakCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.solvedDate(),
                    event.quizCount(),
                    event.topicIds()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak. gradingLogId={}", event.gradingLogId(), e);
        }
    }
}