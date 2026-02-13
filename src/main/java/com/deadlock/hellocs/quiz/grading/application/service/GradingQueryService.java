package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
@Validated
public class GradingQueryService implements QueryGradingLogInputPort {
    
    private final QueryGradingLogOutputPort queryGradingLogPort;
    // TODO: Output 포트 써도 되나 점검. 같은모듈이라 상관없나? 안되면 Input 포트로 구혆
    private final QueryQuizOutputPort queryQuizOutputPort;

    @Override
    public GradingLogResult getGradingLog(@Valid GetGradingLogCommand command) {
        if (command == null) {
            throw new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID);
        }
        String gradingLogId = command.gradingLogId();

        GradingLog gradingLog = queryGradingLogPort.findById(gradingLogId);
        List<Long> quizIds = gradingLog.getResults().stream()
                .map(GradingItem::quizId)
                .toList();
        List<Quiz> quizzes = queryQuizOutputPort.findAllByIds(quizIds);

        validateLoadedQuizzes(quizIds, quizzes);
        return GradingLogResult.from(gradingLog, quizzes);
    }

    @Override
    public GradingDetailLogResult getGradingDetailLog(@Valid GetGradingDetailLogCommand command) {
        if (command == null) {
            throw new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID);
        }
        String gradingLogId = command.gradingLogId();
        Long quizId = command.quizId();

        GradingLog gradingLog = queryGradingLogPort.findById(gradingLogId);
        GradingItem result = getGradingResult(gradingLog, quizId);
        Quiz quiz = loadQuiz(quizId);

        return GradingDetailLogResult.from(result, quiz);
    }
    // --- Helper Methods ---

    private void validateLoadedQuizzes(List<Long> quizIds, List<Quiz> quizzes) {
        long uniqueQuizIdCount = quizIds.stream().distinct().count();
        if (quizzes.size() != uniqueQuizIdCount) {
            throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
        }
    }

    private Quiz loadQuiz(Long quizId) {
        try {
            return queryQuizOutputPort.findById(quizId);
        } catch (CustomException e) {
            if (e.getErrorCode() == QuizErrorStatus.QUIZ_NOT_FOUND) {
                throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
            }
            throw e;
        }
    }

    private GradingItem getGradingResult(GradingLog gradingLog, Long quizId) {
        return gradingLog.getResults().stream()
                .filter(r -> r.quizId().equals(quizId))
                .findFirst()
                .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_RESULT_NOT_FOUND));
    }
}
