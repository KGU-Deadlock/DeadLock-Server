package com.deadlock.hellocs.event;

import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.ranking.adapter.in.event.RankingGradingCompletedEventListener;
import com.deadlock.hellocs.ranking.application.port.in.ApplyRankingScoreInputPort;
import com.deadlock.hellocs.streak.adapter.in.event.StreakGradingCompletedEventListener;
import com.deadlock.hellocs.streak.application.port.in.RecordStreakInputPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventListenerTest {

    @Mock
    private ApplyRankingScoreInputPort applyRankingScoreInputPort;

    @Mock
    private RecordStreakInputPort recordStreakInputPort;

    @Mock
    private QueryGradingLogOutputPort queryGradingLogOutputPort;

    @Mock
    private QueryQuizOutputPort queryQuizOutputPort;

    @Test
    void gradingEventTriggersRankingApply() {
        GradingLog gradingLog = GradingLog.builder()
                .id("log-1")
                .userId(1L)
                .solvedAt(LocalDateTime.now())
                .results(List.of(GradingItem.builder().quizId(10L).score(10).isCorrect(true).build()))
                .build();
        when(queryGradingLogOutputPort.findById("log-1")).thenReturn(gradingLog);
        Quiz quiz = mock(Quiz.class);
        when(quiz.getTopicIds()).thenReturn(List.of(1L, 2L));
        when(queryQuizOutputPort.findAllByIds(List.of(10L))).thenReturn(List.of(quiz));

        RankingGradingCompletedEventListener listener = new RankingGradingCompletedEventListener(
                applyRankingScoreInputPort,
                queryGradingLogOutputPort,
                queryQuizOutputPort
        );

        listener.handle(new GradingCompletedEvent("log-1", 1L, LocalDateTime.now(), 1, 10));

        verify(applyRankingScoreInputPort, times(1)).apply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void gradingEventTriggersStreakRecord() {
        GradingLog gradingLog = GradingLog.builder()
                .id("log-2")
                .userId(1L)
                .solvedAt(LocalDateTime.now())
                .results(List.of(GradingItem.builder().quizId(11L).score(10).isCorrect(true).build()))
                .build();
        when(queryGradingLogOutputPort.findById("log-2")).thenReturn(gradingLog);
        Quiz quiz = mock(Quiz.class);
        when(quiz.getTopicIds()).thenReturn(List.of(3L));
        when(queryQuizOutputPort.findAllByIds(List.of(11L))).thenReturn(List.of(quiz));

        StreakGradingCompletedEventListener listener = new StreakGradingCompletedEventListener(
                recordStreakInputPort,
                queryGradingLogOutputPort,
                queryQuizOutputPort
        );

        listener.handle(new GradingCompletedEvent("log-2", 1L, LocalDateTime.now(), 2, 20));

        verify(recordStreakInputPort, times(1)).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void duplicateGradingLogIdPassesThroughAndKeepsIdempotencyKey() {
        GradingLog gradingLog = GradingLog.builder()
                .id("log-3")
                .userId(1L)
                .solvedAt(LocalDateTime.now())
                .results(List.of(GradingItem.builder().quizId(12L).score(10).isCorrect(true).build()))
                .build();
        when(queryGradingLogOutputPort.findById("log-3")).thenReturn(gradingLog);
        Quiz quiz = mock(Quiz.class);
        when(quiz.getTopicIds()).thenReturn(List.of(1L));
        when(queryQuizOutputPort.findAllByIds(List.of(12L))).thenReturn(List.of(quiz));

        RankingGradingCompletedEventListener listener = new RankingGradingCompletedEventListener(
                applyRankingScoreInputPort,
                queryGradingLogOutputPort,
                queryQuizOutputPort
        );

        listener.handle(new GradingCompletedEvent("log-3", 1L, LocalDateTime.now(), 1, 10));
        listener.handle(new GradingCompletedEvent("log-3", 1L, LocalDateTime.now(), 1, 10));

        ArgumentCaptor<com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand> captor =
                ArgumentCaptor.forClass(com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand.class);
        verify(applyRankingScoreInputPort, times(2)).apply(captor.capture());
        assertEquals("log-3", captor.getValue().gradingLogId());
    }
}
