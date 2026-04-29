package com.deadlock.hellocs.streak.adapter.out.persistence.entity;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * 일일 스트릭 기록의 MongoDB 엔티티.
 *
 * <p>하루 단위로 사용자의 퀴즈 풀이 현황을 기록하며,
 * {@code (userId, date)} 복합 유니크 인덱스로 중복 저장을 방지함.
 * 낙관적 락({@code @Version})을 적용하여 동시 업데이트 충돌을 감지함.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "daily_streak_records")
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_date", def = "{'userId': 1, 'date': -1}", unique = true)
})
public class DailyStreakRecordMongoEntity {

    @Id
    private String id;

    private Long userId;

    private LocalDate date;

    /** 당일 풀이한 퀴즈 수 (누적 가산됨). */
    private int quizCount;

    /** 당일 하루가 끝날 시점의 연속 스트릭 일수. */
    private int streakAtEndOfDay;

    /** 중복 채점 로그 방지용 — 이미 반영된 gradingLogId를 관리함. */
    private Set<String> appliedGradingLogIds;

    /** 낙관적 락 버전 필드. 동시 저장 시 충돌을 감지하는 데 사용됨. */
    @Version
    private Long version;

    public DailyStreakRecord toDomain() {
        return DailyStreakRecord.builder()
                .id(id)
                .userId(userId)
                .date(date)
                .quizCount(quizCount)
                .streakAtEndOfDay(streakAtEndOfDay)
                .appliedGradingLogIds(appliedGradingLogIds == null ? new HashSet<>() : new HashSet<>(appliedGradingLogIds))
                .version(version)
                .build();
    }

    public static DailyStreakRecordMongoEntity from(DailyStreakRecord record) {
        return DailyStreakRecordMongoEntity.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .date(record.getDate())
                .quizCount(record.getQuizCount())
                .streakAtEndOfDay(record.getStreakAtEndOfDay())
                .appliedGradingLogIds(record.getAppliedGradingLogIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(record.getAppliedGradingLogIds()))
                .version(record.getVersion())
                .build();
    }
}
