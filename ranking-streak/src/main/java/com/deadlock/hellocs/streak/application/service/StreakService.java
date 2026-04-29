package com.deadlock.hellocs.streak.application.service;

import com.deadlock.hellocs.streak.application.port.in.QueryStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.RecordStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.DailyStreakRecordResult;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import com.deadlock.hellocs.streak.application.port.out.LoadStreakPort;
import com.deadlock.hellocs.streak.application.port.out.SaveStreakPort;
import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 스트릭 모듈의 핵심 비즈니스 로직.
 *
 * <p>인바운드 포트 두 개({@link RecordStreakUseCase}, {@link QueryStreakUseCase})를 모두 구현하며,
 * 실제 저장소 접근은 아웃바운드 포트({@link LoadStreakPort}, {@link SaveStreakPort})에 위임함.</p>
 *
 * <h3>핵심 흐름</h3>
 * <ul>
 *     <li>record: 채점 완료 이벤트 수신 시, 일일 기록과 사용자 스트릭에 풀이 결과를 반영함.</li>
 *     <li>getSummary: 현재 연속 일수·총 풀이 수·분야 수를 반환함.</li>
 *     <li>getDetail: 요약 정보 + 최장 스트릭·오늘 풀이 여부·이번 달 통계를 반환함.</li>
 *     <li>getMonthly: 특정 연월의 일자별 스트릭 캘린더(기록 없는 날 포함)를 반환함.</li>
 * </ul>
 */
@Service
@Validated
@RequiredArgsConstructor
public class StreakService implements RecordStreakUseCase, QueryStreakUseCase {

    private final LoadStreakPort loadStreakPort;
    private final SaveStreakPort saveStreakPort;

    // 채점 완료 이벤트 수신 시 일일 기록 및 유저 스트릭에 풀이 결과를 반영함. 중복 채점 로그는 무시함.
    @Override
    public void record(RecordStreakCommand command) {
        DailyStreakRecord daily = loadOrCreateDailyRecord(command.userId(), command.solvedDate());
        if (daily.isAlreadyApplied(command.gradingLogId())) {
            return;
        }

        UserStreak userStreak = loadOrCreateUserStreak(command.userId());
        applySolve(userStreak, daily, command);

        saveStreakPort.saveDailyRecord(daily);
        saveStreakPort.saveUserStreak(userStreak);
    }

    // 현재 연속 스트릭 일수, 총 풀이 수, 학습한 토픽 수를 반환함.
    @Override
    public StreakSummaryResult getSummary(Long userId) {
        UserStreak userStreak = loadOrCreateUserStreak(userId);
        LocalDate today = LocalDate.now();

        return new StreakSummaryResult(
                userStreak.getCurrentStreakDays(today),
                userStreak.getTotalSolved(),
                userStreak.getSolvedTopicCount()
        );
    }

    // Summary 정보에 더해 최장 스트릭, 마지막 풀이일, 오늘 풀이 여부, 이번 달 활동 일수·문제 수를 반환함.
    @Override
    public StreakDetailResult getDetail(Long userId) {
        UserStreak userStreak = loadOrCreateUserStreak(userId);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        return new StreakDetailResult(
                userStreak.getCurrentStreakDays(today),
                userStreak.getTotalSolved(),
                userStreak.getSolvedTopicCount(),
                userStreak.getLongestStreak(),
                userStreak.getLastSolvedDate(),
                isSolvedOn(userId, today),
                loadStreakPort.countActiveDays(userId, currentMonth),
                loadStreakPort.sumQuizCount(userId, currentMonth)
        );
    }

    // 특정 연월의 달력 뷰를 반환함. 기록이 없는 날도 포함해 해당 월의 모든 날짜를 채움.
    @Override
    public StreakMonthlyResult getMonthly(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Map<LocalDate, DailyStreakRecord> recordsByDate = loadDailyRecordsAsMap(userId, yearMonth);

        List<DailyStreakRecordResult> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            days.add(toDailyResult(date, recordsByDate.get(date)));
        }
        return new StreakMonthlyResult(year, month, days);
    }

    // DB에서 UserStreak을 조회하고, 없으면 새로 생성함.
    private UserStreak loadOrCreateUserStreak(Long userId) {
        return loadStreakPort.loadUserStreakByUserId(userId)
                .orElseGet(() -> UserStreak.create(userId));
    }

    // 특정 날짜의 DailyStreakRecord를 조회하고, 없으면 새로 생성함.
    private DailyStreakRecord loadOrCreateDailyRecord(Long userId, LocalDate date) {
        return loadStreakPort.loadDailyRecord(userId, date)
                .orElseGet(() -> DailyStreakRecord.create(userId, date));
    }

    // 해당 날짜에 풀이 기록이 존재하는지 여부를 반환함.
    private boolean isSolvedOn(Long userId, LocalDate date) {
        return loadStreakPort.loadDailyRecord(userId, date)
                .map(DailyStreakRecord::hasSolved)
                .orElse(false);
    }

    // 월간 일일 기록 목록을 날짜 기준 Map으로 변환해 반환함.
    private Map<LocalDate, DailyStreakRecord> loadDailyRecordsAsMap(Long userId, YearMonth yearMonth) {
        return loadStreakPort
                .loadDailyRecordsBetween(userId, yearMonth.atDay(1), yearMonth.atEndOfMonth()).stream()
                .collect(Collectors.toMap(DailyStreakRecord::getDate, r -> r));
    }

    // UserStreak과 DailyStreakRecord에 풀이 결과를 적용함.
    private void applySolve(UserStreak userStreak, DailyStreakRecord daily, RecordStreakCommand command) {
        userStreak.applySolved(
                command.solvedDate(),
                command.quizCount(),
                command.topicIds()
        );
        daily.apply(
                command.gradingLogId(),
                command.quizCount(),
                userStreak.getCurrentStreak()
        );
    }

    // DailyStreakRecord를 응답 DTO로 변환함. 기록이 없는 날짜는 미풀이 상태의 빈 결과를 반환함.
    private DailyStreakRecordResult toDailyResult(LocalDate date, DailyStreakRecord record) {
        if (record == null) {
            return new DailyStreakRecordResult(date.toString(), false, 0, 0);
        }
        return new DailyStreakRecordResult(
                date.toString(),
                record.hasSolved(),
                record.getQuizCount(),
                record.getStreakAtEndOfDay()
        );
    }
}
