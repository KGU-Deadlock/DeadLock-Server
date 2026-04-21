package com.deadlock.hellocs.streak.adapter.out.persistence.entity;

import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "user_streaks")
public class UserStreakMongoEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    private int currentStreak;

    private int longestStreak;

    private int totalSolved;

    private Set<Long> solvedTopicIds;

    private LocalDate lastSolvedDate;

    @Version
    private Long version;

    public UserStreak toDomain() {
        return UserStreak.builder()
                .id(id)
                .userId(userId)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .totalSolved(totalSolved)
                .solvedTopicIds(solvedTopicIds == null ? new HashSet<>() : new HashSet<>(solvedTopicIds))
                .lastSolvedDate(lastSolvedDate)
                .version(version)
                .build();
    }

    public static UserStreakMongoEntity from(UserStreak userStreak) {
        return UserStreakMongoEntity.builder()
                .id(userStreak.getId())
                .userId(userStreak.getUserId())
                .currentStreak(userStreak.getCurrentStreak())
                .longestStreak(userStreak.getLongestStreak())
                .totalSolved(userStreak.getTotalSolved())
                .solvedTopicIds(userStreak.getSolvedTopicIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(userStreak.getSolvedTopicIds()))
                .lastSolvedDate(userStreak.getLastSolvedDate())
                .version(userStreak.getVersion())
                .build();
    }
}
