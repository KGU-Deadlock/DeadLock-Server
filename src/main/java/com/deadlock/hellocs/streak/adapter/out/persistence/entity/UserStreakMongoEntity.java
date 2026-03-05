package com.deadlock.hellocs.streak.adapter.out.persistence.entity;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_streaks")
public class UserStreakMongoEntity {
    @Id
    private String id;
    private Long userId;
    private int currentStreak;
    private int longestStreak;
    private int totalSolved;
    @Builder.Default
    private Map<String, DailyStreakRecordMongoValue> dailyRecords = new HashMap<>();
    private LocalDate lastSolvedDate;
    @Version
    private Long version;

    public UserStreak toDomain() {
        Map<String, DailyStreakRecordMongoValue> records = dailyRecords == null ? Map.of() : dailyRecords;
        return UserStreak.builder()
                .id(id)
                .userId(userId)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .totalSolved(totalSolved)
                .dailyRecords(records.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> LocalDate.parse(entry.getKey()),
                                entry -> entry.getValue().toDomain()
                        )))
                .lastSolvedDate(lastSolvedDate)
                .version(version)
                .build();
    }

    public static UserStreakMongoEntity from(UserStreak userStreak) {
        Map<String, DailyStreakRecordMongoValue> records = userStreak.getDailyRecords().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> DailyStreakRecordMongoValue.from(entry.getValue())
                ));

        return UserStreakMongoEntity.builder()
                .id(userStreak.getId())
                .userId(userStreak.getUserId())
                .currentStreak(userStreak.getCurrentStreak())
                .longestStreak(userStreak.getLongestStreak())
                .totalSolved(userStreak.getTotalSolved())
                .dailyRecords(records)
                .lastSolvedDate(userStreak.getLastSolvedDate())
                .version(userStreak.getVersion())
                .build();
    }
}
