package com.deadlock.hellocs.streak.application.service;

import com.deadlock.hellocs.streak.application.port.in.QueryStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.RecordStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.DailyStreakRecordResult;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import com.deadlock.hellocs.streak.application.port.out.LoadStreakPort;
import com.deadlock.hellocs.streak.application.port.out.SaveStreakPort;
import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class StreakService implements RecordStreakInputPort, QueryStreakInputPort {

    private static final int MAX_RETRY_COUNT = 3;

    private final LoadStreakPort loadStreakPort;
    private final SaveStreakPort saveStreakPort;
    // 수정 표시
    private final QueryGradingLogOutputPort queryGradingLogOutputPort;
    // 수정 표시
    private final QueryQuizOutputPort queryQuizOutputPort;

    @Override
    public void record(@Valid RecordStreakCommand command) {
        int attempt = 0;
        while (attempt < MAX_RETRY_COUNT) {
            try {
                // 수정 표시
                var gradingLog = queryGradingLogOutputPort.findById(command.gradingLogId());
                var topicIds = queryQuizOutputPort.findAllByIds(
                                gradingLog.getResults().stream().map(result -> result.getQuizId()).toList())
                        .stream()
                        .flatMap(quiz -> quiz.getTopicIds().stream())
                        .distinct()
                        .toList();

                UserStreak userStreak = loadStreakPort.loadByUserId(command.userId())
                        .orElseGet(() -> UserStreak.create(command.userId()));

                userStreak.applySolved(
                        command.solvedDate(),
                        command.quizCount(),
                        command.gradingLogId(),
                        topicIds
                );
                saveStreakPort.save(userStreak);
                return;
            } catch (OptimisticLockingFailureException e) {
                attempt += 1;
                if (attempt >= MAX_RETRY_COUNT) {
                    throw e;
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StreakSummaryResult getSummary(@NotNull Long userId) {
        UserStreak userStreak = loadStreakPort.loadByUserId(userId)
                .orElseGet(() -> UserStreak.create(userId));
        LocalDate today = LocalDate.now();

        return new StreakSummaryResult(
                userStreak.getCurrentStreakDays(today),
                userStreak.getTotalSolved(),
                userStreak.getSolvedTopicCount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StreakDetailResult getDetail(@NotNull Long userId) {
        UserStreak userStreak = loadStreakPort.loadByUserId(userId)
                .orElseGet(() -> UserStreak.create(userId));
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        return new StreakDetailResult(
                userStreak.getCurrentStreakDays(today),
                userStreak.getTotalSolved(),
                userStreak.getSolvedTopicCount(),
                userStreak.getLongestStreak(),
                userStreak.getLastSolvedDate(),
                userStreak.isSolvedOn(today),
                userStreak.countActiveDays(currentMonth),
                userStreak.countSolvedQuizCount(currentMonth)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StreakMonthlyResult getMonthly(
            @NotNull Long userId,
            @Min(2000) int year,
            @Min(1) @Max(12) int month
    ) {
        UserStreak userStreak = loadStreakPort.loadByUserId(userId)
                .orElseGet(() -> UserStreak.create(userId));
        YearMonth yearMonth = YearMonth.of(year, month);

        List<DailyStreakRecordResult> dailyRecords = new ArrayList<>();
        int streakAtEndOfDay = 0;
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DailyStreakRecord record = userStreak.getDailyRecords().getOrDefault(
                    date,
                    new DailyStreakRecord(false, 0, 0, List.of(), List.of())
            );

            if (record.solved()) {
                streakAtEndOfDay += 1;
            } else {
                streakAtEndOfDay = 0;
            }

            dailyRecords.add(new DailyStreakRecordResult(
                    date.toString(),
                    record.solved(),
                    record.quizCount(),
                    streakAtEndOfDay
            ));
        }

        return new StreakMonthlyResult(
                year,
                month,
                dailyRecords
        );
    }
}
