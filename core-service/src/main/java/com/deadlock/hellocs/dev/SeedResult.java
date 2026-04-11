package com.deadlock.hellocs.dev;

public record SeedResult(
        int topicsCreated,
        int usersCreated,
        int quizzesCreated,
        int rankingEntriesCreated,
        int streaksCreated
) {
}
