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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * dev 서비스가 호출하는 스트릭 기록 시딩 엔드포인트 (세그먼트 분포 모델).
 *
 * <p>DevGradingSeedController와 동일한 결정론적 활동 캘린더를 재현하여
 * user_streaks 와 daily_streak_records 를 직접 bulk write한다.
 * RabbitMQ 이벤트를 경유하지 않으므로 빠르고 확정적이다.</p>
 *
 * <p>호출 전 grading-logs 시드가 완료되어야 gradingLogId 참조가 일관됨.
 * 단, 스트릭 계산 자체는 독립적으로 수행되므로 순서 의존성은 없다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevStreakSeedController {

    private static final String DEV_SEED_PREFIX = "dev-seed-";
    private static final int    BATCH_SIZE      = 1000;

    private final MongoTemplate mongoTemplate;

    /**
     * 스트릭 기록 시딩.
     * grading 시드와 동일한 파라미터를 사용해 동일한 활동 캘린더를 재현한다.
     */
    @PostMapping("/streak-records")
    public ApiResponse<SeedStreakResult> seedStreakRecords(
            @RequestParam(name = "users",            defaultValue = "100")  int   users,
            @RequestParam(name = "signupWindowDays", defaultValue = "180")  int   signupWindowDays,
            @RequestParam(name = "quizPerDay",       defaultValue = "30")   int   quizPerDay,
            @RequestParam(name = "numTopics",        defaultValue = "6")    int   numTopics,
            @RequestParam(name = "segPowerShare",    defaultValue = "0.2")  float segPowerShare,
            @RequestParam(name = "segRegularShare",  defaultValue = "0.5")  float segRegularShare,
            @RequestParam(name = "segPowerDpw",      defaultValue = "7")    int   segPowerDpw,
            @RequestParam(name = "segRegularDpw",    defaultValue = "4")    int   segRegularDpw,
            @RequestParam(name = "segCasualDpw",     defaultValue = "2")    int   segCasualDpw,
            @RequestParam(name = "tokenPoolSize",    defaultValue = "1000") int   tokenPoolSize) {

        SegmentLayout layout = new SegmentLayout(users, tokenPoolSize, segPowerShare, segRegularShare);
        LocalDate today = LocalDate.now();

        List<DailyStreakRecordMongoEntity> dailyBatch = new ArrayList<>(BATCH_SIZE * 200);
        List<UserStreakMongoEntity>        streakBatch = new ArrayList<>(BATCH_SIZE);

        int totalDaily  = 0;
        int totalStreak = 0;

        for (int idx = 0; idx < users; idx++) {
            long kakaoId    = 1001L + idx;
            int  dpw        = layout.dpwOf(idx, segPowerDpw, segRegularDpw, segCasualDpw);
            int  accountAge = layout.accountAgeDays(idx, signupWindowDays);

            UserStreak userStreak = UserStreak.create(kakaoId);

            // 활동 날짜를 오래된 순(oldest-first)으로 처리해 연속 스트릭 계산이 올바르게 누적
            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= dpw) continue;

                LocalDate date        = today.minusDays(d);
                String    gradingLogId = DEV_SEED_PREFIX + kakaoId + "-" + date;
                long      topicId     = (long)((int)(kakaoId + d) % numTopics) + 1L;
                List<Long> topicIds   = List.of(topicId);

                // UserStreak에 풀이 결과 반영 (스트릭 연속일 계산)
                userStreak.applySolved(date, quizPerDay, topicIds);

                // 일일 기록 생성
                DailyStreakRecord daily = DailyStreakRecord.create(kakaoId, date);
                daily.apply(gradingLogId, quizPerDay, userStreak.getCurrentStreak());

                dailyBatch.add(DailyStreakRecordMongoEntity.from(daily));
                totalDaily++;
            }

            // UserStreak 최종 상태 저장
            if (userStreak.getTotalSolved() > 0) {
                streakBatch.add(UserStreakMongoEntity.from(userStreak));
                totalStreak++;
            }

            // 배치 플러시
            if (dailyBatch.size() >= BATCH_SIZE * 200 || idx == users - 1) {
                flushBatches(dailyBatch, streakBatch);
                log.info("[DevStreakSeedController] 진행: idx={}/{}, daily={}", idx + 1, users, totalDaily);
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

    // ─── 세그먼트 레이아웃 헬퍼 (DevGradingSeedController와 동일 알고리즘) ──────

    static final class SegmentLayout {

        private final int users, tokenPoolSize;
        private final int poolPower, poolRegular, poolCasual;
        private final int totalPower, totalRegular;
        private final int bgPower, bgRegular;

        SegmentLayout(int users, int tokenPoolSize, float powerShare, float regularShare) {
            this.users         = users;
            this.tokenPoolSize = Math.min(tokenPoolSize, users);
            this.poolPower     = (int) (this.tokenPoolSize * powerShare);
            this.poolRegular   = (int) (this.tokenPoolSize * regularShare);
            this.poolCasual    = this.tokenPoolSize - this.poolPower - this.poolRegular;
            this.totalPower    = (int) (users * powerShare);
            this.totalRegular  = (int) (users * regularShare);
            this.bgPower       = Math.max(0, totalPower   - poolPower);
            this.bgRegular     = Math.max(0, totalRegular - poolRegular);
        }

        int dpwOf(int idx, int powerDpw, int regularDpw, int casualDpw) {
            return switch (segmentOf(idx)) {
                case POWER   -> powerDpw;
                case REGULAR -> regularDpw;
                case CASUAL  -> casualDpw;
            };
        }

        int accountAgeDays(int idx, int signupWindowDays) {
            int[] info    = segmentIdxAndSize(idx);
            int segIdx    = info[0];
            int segSize   = info[1];
            if (segSize <= 1) return signupWindowDays;
            return (segSize - 1 - segIdx) * signupWindowDays / (segSize - 1);
        }

        private Segment segmentOf(int idx) {
            if (idx < poolPower)                   return Segment.POWER;
            if (idx < poolPower + poolRegular)     return Segment.REGULAR;
            if (idx < tokenPoolSize)               return Segment.CASUAL;
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)                   return Segment.POWER;
            if (bgIdx < bgPower + bgRegular)       return Segment.REGULAR;
            return Segment.CASUAL;
        }

        private int[] segmentIdxAndSize(int idx) {
            if (idx < poolPower)
                return new int[]{ idx, poolPower };
            if (idx < poolPower + poolRegular)
                return new int[]{ idx - poolPower, poolRegular };
            if (idx < tokenPoolSize)
                return new int[]{ idx - poolPower - poolRegular, poolCasual };
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)
                return new int[]{ bgIdx, bgPower };
            if (bgIdx < bgPower + bgRegular)
                return new int[]{ bgIdx - bgPower, bgRegular };
            int bgCasual = users - tokenPoolSize - bgPower - bgRegular;
            return new int[]{ bgIdx - bgPower - bgRegular, Math.max(1, bgCasual) };
        }

        enum Segment { POWER, REGULAR, CASUAL }
    }

    // ─── 결과 레코드 ─────────────────────────────────────────────────────────

    public record SeedStreakResult(int dailyRecordsCreated, int userStreaksCreated) {}
}
