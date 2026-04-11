package com.deadlock.hellocs.quiz.grading.application.service;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GradingQueryServiceTest {

    @Mock
    private QueryGradingLogOutputPort queryGradingLogPort;

    @Mock
    private QueryQuizOutputPort queryQuizOutputPort;

    @InjectMocks
    private GradingQueryService gradingQueryService;

    @Test
    @DisplayName("grading log list is sorted by solvedAt descending")
    void getGradingLogList_sortsBySolvedAtDesc() {
        Long userId = 12345L;
        GradingLog older = gradingLog("older", userId, LocalDateTime.of(2026, 3, 20, 10, 0));
        GradingLog latest = gradingLog("latest", userId, LocalDateTime.of(2026, 3, 26, 18, 30));
        GradingLog middle = gradingLog("middle", userId, LocalDateTime.of(2026, 3, 24, 9, 15));

        given(queryGradingLogPort.findAllByUserId(userId)).willReturn(List.of(older, latest, middle));

        List<GradingLogListResult> result = gradingQueryService.getGradingLogList(userId);

        assertThat(result).extracting(GradingLogListResult::id)
                .containsExactly("latest", "middle", "older");
    }

    private GradingLog gradingLog(String id, Long userId, LocalDateTime solvedAt) {
        return GradingLog.builder()
                .id(id)
                .userId(userId)
                .solvedAt(solvedAt)
                .totalCount(3)
                .correctCount(2)
                .quizMode(QuizMode.STANDARD)
                .topicNames(List.of("OS", "Network"))
                .results(List.of())
                .build();
    }
}
