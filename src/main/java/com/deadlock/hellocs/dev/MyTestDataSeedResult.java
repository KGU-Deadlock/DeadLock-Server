package com.deadlock.hellocs.dev;

public record MyTestDataSeedResult(
        Long userId,
        String nickname,
        int topicCount,
        int quizzesCreated,
        int globalRankingEntriesUpserted,
        int topicRankingEntriesUpserted,
        int streakDaysCreated
) {
}
