package com.deadlock.hellocs.quiz.grading.adapter.out.persistence;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MongoDB 기반 채점 로그 영속성 어댑터. {@link GradingLogMongoEntity}와 도메인 객체 간 변환을 처리함.
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
                .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_LOG_NOT_FOUND));
    }

    @Override
    public List<GradingLog> findAllByUserId(Long userId) {
        return gradingLogRepository.findAllByUserId(userId).stream()
                .map(GradingLogMongoEntity::toDomain)
                .toList();
    }
}
