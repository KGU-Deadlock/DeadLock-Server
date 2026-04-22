package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import jakarta.validation.Valid;

/**
 * 스트릭 기록(Command) 인바운드 포트.
 *
 * <p>채점 완료 이벤트에 의해 호출되며, 일일 기록과 사용자 스트릭을 갱신함.</p>
 */
public interface RecordStreakUseCase {

    /** 채점 결과를 일일 기록 및 사용자 스트릭에 반영함. 중복 gradingLogId는 무시됨. */
    void record(@Valid RecordStreakCommand command);
}
