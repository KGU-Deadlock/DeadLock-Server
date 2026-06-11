package com.deadlock.hellocs.streak.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoEntity;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevStreakSeedController {

    private static final String DEV_SEED_PREFIX = "dev-seed-";
    private static final int    BATCH_SIZE      = 1000;

    private final MongoTemplate mongoTemplate;

    @PostMapping("/streak-records")
    public ApiResponse<SeedStreakResult> seedStreakRecords(@RequestBody SeedRequest request) {

        LocalDate      today      = LocalDate.now();
        List<UserSpec> specs      = request.users();

        List<DailyStreakRecordMongoEntity> dailyBatch  = new ArrayList<>(BATCH_SIZE * 200);
        List<UserStreakMongoEntity>        streakBatch = new ArrayList<>(BATCH_SIZE);

        int totalDaily  = 0;
        int totalStreak = 0;

        for (int idx = 0; idx < specs.size(); idx++) {
            UserSpec spec    = specs.get(idx);
            long  kakaoId    = spec.kakaoId();
            int   accountAge = spec.accountAgeDays();

            UserStreak userStreak = UserStreak.create(kakaoId);

            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= spec.dpw()) continue;

                LocalDate  date         = today.minusDays(d);
                String     gradingLogId = DEV_SEED_PREFIX + kakaoId + "-" + date;
                long       topicId      = (long)((int)(kakaoId + d) % request.numTopics()) + 1L;
                List<Long> topicIds     = List.of(topicId);

                userStreak.applySolved(date, request.quizPerDay(), topicIds);

                DailyStreakRecord daily = DailyStreakRecord.create(kakaoId, date);
                daily.apply(gradingLogId, request.quizPerDay(), userStreak.getCurrentStreak());

                dailyBatch.add(DailyStreakRecordMongoEntity.from(daily));
                totalDaily++;
            }

            if (userStreak.getTotalSolved() > 0) {
                streakBatch.add(UserStreakMongoEntity.from(userStreak));
                totalStreak++;
            }

            if (dailyBatch.size() >= BATCH_SIZE * 200 || idx == specs.size() - 1) {
                flushBatches(dailyBatch, streakBatch);
                log.info("[DevStreakSeedController] 진행: idx={}/{}, daily={}", idx + 1, specs.size(), totalDaily);
                dailyBatch.clear();
                streakBatch.clear();
            }
        }

        log.info("[DevStreakSeedController] 완료: dailyRecords={}, userStreaks={}", totalDaily, totalStreak);
        return ApiResponse.onSuccess(new SeedStreakResult(totalDaily, totalStreak));
    }

    private void flushBatches(List<DailyStreakRecordMongoEntity> daily,
                               List<UserStreakMongoEntity>        streaks) {
        if (!daily.isEmpty())   mongoTemplate.insert(daily,   DailyStreakRecordMongoEntity.class);
        if (!streaks.isEmpty()) mongoTemplate.insert(streaks, UserStreakMongoEntity.class);
    }

    // ─── 요청/결과 레코드 ────────────────────────────────────────────────────

    record UserSpec(long kakaoId, int dpw, int accountAgeDays) {}
    record SeedRequest(List<UserSpec> users, int quizPerDay, int numTopics) {}

    public record SeedStreakResult(int dailyRecordsCreated, int userStreaksCreated) {}
}
