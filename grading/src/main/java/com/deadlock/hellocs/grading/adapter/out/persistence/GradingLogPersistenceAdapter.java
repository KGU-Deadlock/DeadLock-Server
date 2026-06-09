package com.deadlock.hellocs.grading.adapter.out.persistence;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import com.deadlock.hellocs.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB 기반 채점 로그 영속성 어댑터.
 */
@Component
@RequiredArgsConstructor
public class GradingLogPersistenceAdapter implements CommandGradingLogOutputPort, QueryGradingLogOutputPort {
    private final GradingLogRepository gradingLogRepository;

    @Override
    public GradingLog save(GradingLog gradingLog) {
        GradingLogMongoEntity savedGradingLog = gradingLogRepository.save(GradingLogMongoEntity.from(gradingLog));
        return savedGradingLog.toDomain();
    }

    @Override
    public GradingLog findById(String id) {
        return gradingLogRepository.findById(id)
                .map(GradingLogMongoEntity::toDomain)
                .orElseThrow(() -> new CustomException(GradingErrorStatus.GRADING_LOG_NOT_FOUND));
    }

    @Override
    public List<GradingLog> findAllByUserId(Long userId) {
        return gradingLogRepository.findAllByUserId(userId).stream()
                .map(GradingLogMongoEntity::toDomain)
                .toList();
    }
}
