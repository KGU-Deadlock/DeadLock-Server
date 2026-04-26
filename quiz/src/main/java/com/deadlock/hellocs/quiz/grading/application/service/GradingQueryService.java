package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;

/**
 * 채점 로그 조회 서비스. 모든 조회 전에 요청자가 로그의 소유자인지 검증함.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class GradingQueryService implements QueryGradingLogInputPort {

    private final QueryGradingLogOutputPort queryGradingLogPort;

    /** 채점 로그 전체 요약(정답 수, 문제 수, 결과 목록)을 반환함. */
    @Override
    public GradingLogResult getGradingLog(Long requesterId, GetGradingLogCommand command) {
        GradingLog gradingLog = queryGradingLogPort.findById(command.gradingLogId());
        validateOwnership(gradingLog, requesterId);
        return GradingLogResult.from(gradingLog);
    }

    /** 특정 퀴즈의 상세 채점 결과(피드백, 누락 키워드, 개선 답변 포함)를 반환함. */
    @Override
    public GradingDetailLogResult getGradingDetailLog(Long requesterId, GetGradingDetailLogCommand command) {
        GradingLog gradingLog = queryGradingLogPort.findById(command.gradingLogId());
        validateOwnership(gradingLog, requesterId);
        GradingItem result = getGradingResult(gradingLog, command.quizId());
        return GradingDetailLogResult.from(result);
    }

    /** 사용자의 모든 채점 로그를 최신순으로 반환함. */
    @Override
    public List<GradingLogListResult> getGradingLogList(Long requesterId) {
        return queryGradingLogPort.findAllByUserId(requesterId).stream()
                .sorted(Comparator.comparing(GradingLog::getSolvedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(GradingLogListResult::from)
                .toList();
    }

    /** 다른 사용자의 채점 로그에 접근하는 경우 {@code GRADING_ACCESS_DENIED} 예외를 던짐. */
    private void validateOwnership(GradingLog gradingLog, Long userId) {
        if (!gradingLog.getUserId().equals(userId)) {
            throw new CustomException(QuizErrorStatus.GRADING_ACCESS_DENIED);
        }
    }

    private GradingItem getGradingResult(GradingLog gradingLog, Long quizId) {
        return gradingLog.getResults().stream()
                .filter(r -> r.quizId().equals(quizId))
                .findFirst()
                .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_RESULT_NOT_FOUND));
    }
}
