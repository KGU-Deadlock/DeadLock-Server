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

/**
 * 사용자 전체 스트릭 통계의 MongoDB 엔티티.
 *
 * <p>사용자별 현재 연속 일수, 최장 연속 일수, 총 풀이 수, 풀이 분야 목록을 누적 관리함.
 * {@code userId}에 유니크 인덱스가 걸려 있어 사용자당 단 하나의 문서만 존재함.</p>
 */
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

    /** 현재 연속 학습 일수. 하루라도 건너뛰면 1로 초기화됨. */
    private int currentStreak;

    /** 역대 최장 연속 학습 일수. */
    private int longestStreak;

    /** 서비스 가입 이후 총 누적 풀이 수. */
    private int totalSolved;

    /** 풀이한 적 있는 주제(topic) ID 집합. */
    private Set<Long> solvedTopicIds;

    /** 가장 최근에 퀴즈를 풀이한 날짜. 스트릭 연속 여부 판단에 사용됨. */
    private LocalDate lastSolvedDate;

    /** 낙관적 락 버전 필드. 동시 저장 시 충돌을 감지하는 데 사용됨. */
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
