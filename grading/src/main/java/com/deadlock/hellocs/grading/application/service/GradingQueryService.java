package com.deadlock.hellocs.grading.application.service;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.grading.domain.GradingLog;
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

    @Override
    public GradingLogResult getGradingLog(Long requesterId, GetGradingLogCommand command) {
        GradingLog gradingLog = queryGradingLogPort.findById(command.gradingLogId());
        validateOwnership(gradingLog, requesterId);
        return GradingLogResult.from(gradingLog);
    }

    @Override
    public GradingDetailLogResult getGradingDetailLog(Long requesterId, GetGradingDetailLogCommand command) {
        GradingLog gradingLog = queryGradingLogPort.findById(command.gradingLogId());
        validateOwnership(gradingLog, requesterId);
        GradingItem result = getGradingResult(gradingLog, command.quizId());
        return GradingDetailLogResult.from(result);
    }

    @Override
    public List<GradingLogListResult> getGradingLogList(Long requesterId) {
        return queryGradingLogPort.findAllByUserId(requesterId).stream()
                .sorted(Comparator.comparing(GradingLog::getSolvedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(GradingLogListResult::from)
                .toList();
    }

    private void validateOwnership(GradingLog gradingLog, Long userId) {
        if (!gradingLog.getUserId().equals(userId)) {
            throw new CustomException(GradingErrorStatus.GRADING_ACCESS_DENIED);
        }
    }

    private GradingItem getGradingResult(GradingLog gradingLog, Long quizId) {
        return gradingLog.getResults().stream()
                .filter(r -> r.quizId().equals(quizId))
                .findFirst()
                .orElseThrow(() -> new CustomException(GradingErrorStatus.GRADING_RESULT_NOT_FOUND));
    }
}
