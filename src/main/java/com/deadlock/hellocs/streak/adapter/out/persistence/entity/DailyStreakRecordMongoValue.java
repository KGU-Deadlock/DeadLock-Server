package com.deadlock.hellocs.streak.adapter.out.persistence.entity;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyStreakRecordMongoValue {
    // 수정 표시
    private boolean solved;
    // 수정 표시
    private int quizCount;
    // 수정 표시
    private int streakAtEndOfDay;
    // 수정 표시
    private List<Long> topicIds;
    // 수정 표시
    private List<String> appliedGradingLogIds;

    public DailyStreakRecord toDomain() {
        return new DailyStreakRecord(
                solved,
                quizCount,
                streakAtEndOfDay,
                topicIds == null ? List.of() : topicIds,
                appliedGradingLogIds == null ? List.of() : appliedGradingLogIds
        );
    }

    public static DailyStreakRecordMongoValue from(DailyStreakRecord dailyStreakRecord) {
        return new DailyStreakRecordMongoValue(
                dailyStreakRecord.solved(),
                dailyStreakRecord.quizCount(),
                dailyStreakRecord.streakAtEndOfDay(),
                dailyStreakRecord.topicIds(),
                dailyStreakRecord.appliedGradingLogIds()
        );
    }
}
