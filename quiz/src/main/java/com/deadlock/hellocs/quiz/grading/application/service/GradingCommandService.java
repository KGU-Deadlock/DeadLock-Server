package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.quiz.grading.application.policy.GradingPolicy;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
// TODO: topic 모듈이 별도 서브모듈로 분리되면 project(':topic') 의존성 추가 및 컴파일 에러 해소 필요
import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
@Validated
public class GradingCommandService implements CommandAnswerInputPort {
    
    private final QueryQuizOutputPort queryQuizPort;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final List<GradingPolicy> gradingPolicies;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LoadTopicUseCase loadTopicUseCase;
    
    @Override
    public String submit(SubmitAnswersCommand command) {
        if (command == null) {
            throw new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID);
        }
        Long userId = command.userId();
        List<UserGradingCommand> answers = command.answers();

        // 1. 퀴즈 ID 추출
        List<Long> quizIds = extractQuizIds(answers);
        validateQuizIds(quizIds);

        // 2. 퀴즈 조회
        List<Quiz> quizzes = loadQuizzes(quizIds);

        // 3. 채점 수행 (각 퀴즈 타입에 맞는 정책 적용)
        List<GradingItem> gradingItems = gradeAnswers(answers, quizzes);

        // 4. 메타데이터 추론
        QuizMode quizMode = inferQuizMode(quizzes);
        List<Long> topicIds = extractTopicIds(quizzes);
        List<String> topicNames = loadTopicUseCase.getTopicNames(topicIds);

        // 5. 채점 로그 생성 및 저장
        GradingLog gradingLog = GradingLog.create(userId, gradingItems, quizMode, topicNames);
        GradingLog savedGradingLog = commandGradingLogPort.save(gradingLog);

        applicationEventPublisher.publishEvent(new GradingCompletedEvent(
                savedGradingLog.getId(),
                savedGradingLog.getUserId(),
                savedGradingLog.getSolvedAt(),
                savedGradingLog.getTotalCount(),
                savedGradingLog.getResults().stream().mapToInt(GradingItem::score).sum(),
                topicIds
        ));

        return savedGradingLog.getId();
    }

    // --- Helper Methods ---

    private List<Long> extractQuizIds(List<UserGradingCommand> answers) {
        return answers.stream()
                .map(UserGradingCommand::quizId)
                .toList();
    }

    private void validateQuizIds(List<Long> quizIds) {
        long uniqueCount = quizIds.stream().distinct().count();
        if (uniqueCount != quizIds.size()) {
            throw new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID);
        }
    }

    private List<Quiz> loadQuizzes(List<Long> quizIds) {
        List<Quiz> quizzes = queryQuizPort.findAllByIds(quizIds);

        if (quizzes.size() != quizIds.size()) {
            throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
        }
        return quizzes;
    }

    private List<GradingItem> gradeAnswers(List<UserGradingCommand> answers, List<Quiz> quizzes) {
        List<GradingItem> results = new ArrayList<>();
        Map<Long, Quiz> quizMap = quizzes.stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        for (UserGradingCommand userGradingCommand : answers) {
            Quiz quiz = findQuiz(quizMap, userGradingCommand.quizId());
            results.add(gradeAnswer(quiz, userGradingCommand.answer()));
        }
        return results;
    }

    private Quiz findQuiz(Map<Long, Quiz> quizMap, Long quizId) {
        Quiz quiz = quizMap.get(quizId);
        if (quiz == null) {
            throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
        }
        return quiz;
    }

    private GradingItem gradeAnswer(Quiz quiz, String answer) {
        GradingPolicy policy = findGradingPolicy(quiz.getType());
        return policy.grade(quiz, answer);
    }

    private GradingPolicy findGradingPolicy(QuizType quizType) {
        return gradingPolicies.stream()
                .filter(policy -> policy.supports(quizType))
                .findFirst()
                .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_POLICY_NOT_FOUND));
    }

    private QuizMode inferQuizMode(List<Quiz> quizzes) {
        boolean allVoice = quizzes.stream()
                .allMatch(quiz -> quiz.getType() == QuizType.VOICE);
        return allVoice ? QuizMode.VOICE : QuizMode.STANDARD;
    }

    private List<Long> extractTopicIds(List<Quiz> quizzes) {
        return quizzes.stream()
                .flatMap(quiz -> quiz.getTopicIds().stream())
                .distinct()
                .toList();
    }
}
