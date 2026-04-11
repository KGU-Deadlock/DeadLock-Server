package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.quiz.grading.application.policy.GradingPolicy;
import com.deadlock.hellocs.quiz.grading.application.policy.StandardGradingPolicy;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GradingCommandServiceTest {

    @Mock private QueryQuizOutputPort queryQuizPort;
    @Mock private CommandGradingLogOutputPort commandGradingLogPort;
    @Mock private KafkaTemplate<String, GradingCompletedEventMessage> kafkaTemplate;
    @Mock private LoadTopicUseCase loadTopicUseCase;

    @InjectMocks
    private GradingCommandService gradingCommandService;

    // StandardGradingPolicy는 실제 구현 사용 (전략 패턴 검증)
    private final List<GradingPolicy> gradingPolicies = List.of(new StandardGradingPolicy());

    @Test
    @DisplayName("채점 완료 후 Kafka로 GradingCompletedEventMessage 발행")
    void submit_publishesKafkaEvent() {
        // given
        Long userId = 10001L;
        Long quizId = 1L;
        List<Long> topicIds = List.of(100L, 200L);

        Quiz quiz = buildOxQuiz(quizId, "O", topicIds);
        given(queryQuizPort.findAllByIds(List.of(quizId))).willReturn(List.of(quiz));
        given(loadTopicUseCase.getTopicNames(topicIds)).willReturn(List.of("OS", "Network"));

        GradingLog savedLog = GradingLog.create(userId,
                List.of(GradingItem.of(quizId, true, 10)),
                QuizMode.STANDARD,
                List.of("OS", "Network"));
        given(commandGradingLogPort.save(any())).willReturn(savedLog);
        given(kafkaTemplate.send(any(), any(), any())).willReturn(CompletableFuture.completedFuture(null));

        SubmitAnswersCommand command = new SubmitAnswersCommand(userId,
                List.of(new UserGradingCommand(quizId, "O")));

        // when
        gradingCommandService.submit(command);

        // then: Kafka 토픽으로 발행됐는지 검증
        ArgumentCaptor<GradingCompletedEventMessage> captor =
                ArgumentCaptor.forClass(GradingCompletedEventMessage.class);
        verify(kafkaTemplate).send(
                eq(GradingCommandService.TOPIC_GRADING_COMPLETED),
                any(),
                captor.capture()
        );

        GradingCompletedEventMessage published = captor.getValue();
        assertThat(published.userId()).isEqualTo(userId);
        assertThat(published.topicIds()).containsExactlyInAnyOrderElementsOf(topicIds);
        assertThat(published.quizCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("채점 이벤트 메시지에 topicIds가 포함되어 ranking-streak 서비스가 별도 조회 불필요")
    void submit_eventContainsTopicIds() {
        Long userId = 10001L;
        Long quizId = 2L;
        List<Long> topicIds = List.of(300L);

        Quiz quiz = buildOxQuiz(quizId, "X", topicIds);
        given(queryQuizPort.findAllByIds(List.of(quizId))).willReturn(List.of(quiz));
        given(loadTopicUseCase.getTopicNames(topicIds)).willReturn(List.of("Database"));

        GradingLog savedLog = GradingLog.create(userId,
                List.of(GradingItem.of(quizId, false, 0)),
                QuizMode.STANDARD,
                List.of("Database"));
        given(commandGradingLogPort.save(any())).willReturn(savedLog);
        given(kafkaTemplate.send(any(), any(), any())).willReturn(CompletableFuture.completedFuture(null));

        gradingCommandService.submit(new SubmitAnswersCommand(userId,
                List.of(new UserGradingCommand(quizId, "X"))));

        ArgumentCaptor<GradingCompletedEventMessage> captor =
                ArgumentCaptor.forClass(GradingCompletedEventMessage.class);
        verify(kafkaTemplate).send(any(), any(), captor.capture());

        assertThat(captor.getValue().topicIds()).containsExactly(300L);
    }

    private Quiz buildOxQuiz(Long id, String correctAnswer, List<Long> topicIds) {
        return Quiz.builder()
                .id(id)
                .type(QuizType.OX)
                .correctAnswer(correctAnswer)
                .topicIds(topicIds)
                .build();
    }
}
