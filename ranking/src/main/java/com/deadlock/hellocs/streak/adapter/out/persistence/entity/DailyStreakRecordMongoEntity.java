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

    private int quizCount;

    private int streakAtEndOfDay;

    private Set<String> appliedGradingLogIds;

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
