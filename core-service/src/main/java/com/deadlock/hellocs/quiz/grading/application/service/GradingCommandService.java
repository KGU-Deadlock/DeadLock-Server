package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.grading.application.policy.GradingPolicy;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import com.deadlock.hellocs.shared.events.GradingCompletedEventMessage;
import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채점 Command 서비스
 * GradingCompletedEvent를 Spring ApplicationEvent 대신 Kafka로 발행 (MSA 분리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class GradingCommandService implements CommandAnswerInputPort {

    static final String TOPIC_GRADING_COMPLETED = "grading.completed";

    private final QueryQuizOutputPort queryQuizPort;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final List<GradingPolicy> gradingPolicies;
    private final KafkaTemplate<String, GradingCompletedEventMessage> kafkaTemplate;
    private final LoadTopicUseCase loadTopicUseCase;

    @Override
    public String submit(SubmitAnswersCommand command) {
        if (command == null) {
            throw new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID);
        }

        List<Long> quizIds = extractQuizIds(command.answers());
        validateQuizIds(quizIds);

        List<Quiz> quizzes = loadQuizzes(quizIds);
        List<GradingItem> gradingItems = gradeAnswers(command.answers(), quizzes);

        QuizMode quizMode = inferQuizMode(quizzes);
        List<Long> topicIds = collectTopicIds(quizzes);
        List<String> topicNames = loadTopicUseCase.getTopicNames(topicIds);

        GradingLog gradingLog = GradingLog.create(command.userId(), gradingItems, quizMode, topicNames);
        GradingLog savedGradingLog = commandGradingLogPort.save(gradingLog);

        publishGradingCompletedEvent(savedGradingLog, topicIds);

        return savedGradingLog.getId();
    }

    private void publishGradingCompletedEvent(GradingLog gradingLog, List<Long> topicIds) {
        GradingCompletedEventMessage message = new GradingCompletedEventMessage(
                gradingLog.getId(),
                gradingLog.getUserId(),
                gradingLog.getSolvedAt(),
                gradingLog.getTotalCount(),
                gradingLog.getResults().stream().mapToInt(GradingItem::score).sum(),
                topicIds
        );
        kafkaTemplate.send(TOPIC_GRADING_COMPLETED, gradingLog.getId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish grading.completed event. gradingLogId={}", gradingLog.getId(), ex);
                    }
                });
    }

    private List<Long> extractQuizIds(List<UserGradingCommand> answers) {
        return answers.stream().map(UserGradingCommand::quizId).toList();
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
        for (UserGradingCommand cmd : answers) {
            Quiz quiz = quizMap.get(cmd.quizId());
            if (quiz == null) {
                throw new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND);
            }
            GradingPolicy policy = gradingPolicies.stream()
                    .filter(p -> p.supports(quiz.getType()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(QuizErrorStatus.GRADING_POLICY_NOT_FOUND));
            results.add(policy.grade(quiz, cmd.answer()));
        }
        return results;
    }

    private QuizMode inferQuizMode(List<Quiz> quizzes) {
        boolean allVoice = quizzes.stream().allMatch(q -> q.getType() == QuizType.VOICE);
        return allVoice ? QuizMode.VOICE : QuizMode.STANDARD;
    }

    private List<Long> collectTopicIds(List<Quiz> quizzes) {
        return quizzes.stream()
                .filter(q -> q.getTopicIds() != null)
                .flatMap(q -> q.getTopicIds().stream())
                .distinct()
                .toList();
    }
}
