package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 채점 Query 서비스 (Read 담당)
 * 
 * 책임:
 * - 채점 기록 조회
 * - 채점 상세 결과 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingQueryService implements QueryGradingLogInputPort {
    
    private final QueryGradingLogOutputPort queryGradingLogPort;
    // TODO: Output 포트 써도 되나 점검. 같은모듈이라 상관없나? 안되면 Input 포트로 구혆
    private final QueryQuizOutputPort queryQuizOutputPort;

    @Override
    public GradingLogResult getGradingLog(String gradingLogId) {
        GradingLog gradingLog = queryGradingLogPort.findById(gradingLogId);
        List<Long> quizIds = gradingLog.getResults().stream()
                .map(GradingItem::quizId)
                .toList();
        List<Quiz> quizzes = queryQuizOutputPort.findAllByIds(quizIds);
        
        return GradingLogResult.from(gradingLog, quizzes);
    }

    @Override
    public GradingDetailLogResult getGradingDetailLog(String gradingLogId, Long quizId) {
        GradingLog gradingLog = queryGradingLogPort.findById(gradingLogId);
        GradingItem result = getGradingResult(gradingLog, quizId);
        Quiz quiz = queryQuizOutputPort.findById(quizId);

        return GradingDetailLogResult.from(result, quiz);
    }
    // --- Helper Methods ---

    private GradingItem getGradingResult(GradingLog gradingLog, Long quizId) {
        // TODO: DB에서 해당 데이터만 바로 가져오기
        return gradingLog.getResults().stream()
                .filter(r -> r.quizId().equals(quizId))
                .findFirst()
                .get();
    }
}
