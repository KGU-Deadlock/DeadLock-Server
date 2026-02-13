package com.deadlock.hellocs.quiz.grading.adapter.out.persistence;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradingLogPersistenceAdapter implements CommandGradingLogOutputPort, QueryGradingLogOutputPort {
    private final GradingLogRepository gradingLogRepository;

    @Override
    public GradingLog save(GradingLog gradingLog) {
        return gradingLogRepository.save(gradingLog);
    }

    @Override
    public GradingLog findById(String id) {
        return gradingLogRepository.findById(id)
                .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_LOG_NOT_FOUND));
    }
}
