package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.quiz.grading.application.policy.GradingPolicy;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 채점 Command 서비스 (CUD 담당)
 * 
 * 책임:
 * - 답안 제출 및 채점 수행 (Create)
 * - 채점 결과 저장
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GradingCommandService implements CommandAnswerInputPort {
    
    private final QueryQuizOutputPort queryQuizPort;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final List<GradingPolicy> gradingPolicies;
    
    @Override
    public String submit(Long userId, List<UserGradingCommand> answers) {
        // 1. 퀴즈 ID 추출
        List<Long> quizIds = extractQuizIds(answers);

        // 2. 퀴즈 조회
        List<Quiz> quizzes = loadQuizzes(quizIds);

        // 3. 채점 수행 (각 퀴즈 타입에 맞는 정책 적용)
        List<GradingItem> gradingItems = gradeAnswers(answers, quizzes);

        // 4. 채점 로그 생성 및 저장
        GradingLog gradingLog = GradingLog.create(userId, gradingItems);
        return commandGradingLogPort.save(gradingLog).getId();
    }

    // --- Helper Methods ---

    private List<Long> extractQuizIds(List<UserGradingCommand> answers) {
        return answers.stream()
                .map(UserGradingCommand::quizId)
                .toList();
    }

    private List<Quiz> loadQuizzes(List<Long> quizIds) {
        // TODO: 예외 처리 구현 필요 (Quizzes not found or size mismatch)
        return queryQuizPort.findAllByIds(quizIds);
    }

    private List<GradingItem> gradeAnswers(List<UserGradingCommand> answers, List<Quiz> quizzes) {
        List<GradingItem> results = new ArrayList<>();
        for (UserGradingCommand userGradingCommand : answers) {
            Quiz quiz = findQuiz(quizzes, userGradingCommand.quizId());
            results.add(gradeAnswer(quiz, userGradingCommand.answer()));
        }
        return results;
    }

    private Quiz findQuiz(List<Quiz> quizzes, Long quizId) {
        // TODO: 예외 처리 구현 필요 (Quiz not found)
        return quizzes.stream()
                .filter(q -> q.getId().equals(quizId))
                .findFirst()
                .get();
    }

    private GradingItem gradeAnswer(Quiz quiz, String answer) {
        GradingPolicy policy = findGradingPolicy(quiz.getType());
        return policy.grade(quiz, answer);
    }

    private GradingPolicy findGradingPolicy(QuizType quizType) {
        // TODO: 예외 처리 구현 필요 (No grading policy found)
        return gradingPolicies.stream()
                .filter(policy -> policy.supports(quizType))
                .findFirst()
                .get();
    }
}
