package com.deadlock.hellocs.dev;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.QuizRepository;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizMultipleChoiceJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizOxJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizShortAnswerJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizVoiceJpaEntity;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import com.deadlock.hellocs.streak.adapter.out.persistence.UserStreakRepository;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoValue;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import com.deadlock.hellocs.topic.adapter.out.persistence.TopicRepository;
import com.deadlock.hellocs.topic.adapter.out.persistence.entity.TopicJpaEntity;
import com.deadlock.hellocs.user.adapter.out.persistence.UserRepository;
import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaEntity;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final String DEV_SEED_PREFIX = "[DEV-SEED]";
    private static final String GLOBAL_RANKING_KEY = "ranking:global";
    private static final String TOPIC_RANKING_KEY_PREFIX = "ranking:topic:";
    private static final int QUIZZES_PER_TYPE_AND_LEVEL = 5;

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserStreakRepository userStreakRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public SeedResult seed() {
        List<String> topicNames = seedTopicNames();
        SeedTopicsResult seedTopicsResult = seedTopics(topicNames);
        Map<String, TopicJpaEntity> topics = seedTopicsResult.topics();
        List<Long> topicIdsInOrder = topicNames.stream()
                .map(name -> topics.get(name).getId())
                .toList();

        List<SeedUser> seedUsers = seedUsers();
        List<SeedUserResolved> resolvedUsers = seedUsers.stream()
                .map(seedUser -> seedUser.resolve(topicIdsInOrder))
                .toList();

        int usersCreated = seedUsers(resolvedUsers);
        int quizzesCreated = seedQuizzes(topicNames, topicIdsInOrder);
        int rankingEntriesCreated = seedRanking(resolvedUsers);
        int streaksCreated = seedStreaks(resolvedUsers);

        return new SeedResult(
                seedTopicsResult.createdCount(),
                usersCreated,
                quizzesCreated,
                rankingEntriesCreated,
                streaksCreated
        );
    }

    @Transactional
    public MyTestDataSeedResult seedMyData(Long kakaoId) {
        UserJpaEntity user = userRepository.findTopByKakaoIdOrderByIdDesc(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorStatus._USER_NOT_FOUND));

        List<TopicJpaEntity> allTopics = topicRepository.findAll().stream()
                .sorted(Comparator.comparing(TopicJpaEntity::getId))
                .toList();

        List<Long> allTopicIds = allTopics.stream()
                .map(TopicJpaEntity::getId)
                .toList();
        List<String> allTopicNames = allTopics.stream()
                .map(TopicJpaEntity::getName)
                .toList();

        List<Long> rankingTopicIds = allTopicIds.isEmpty()
                ? user.toDomain().getInterestTopicIds()
                : allTopicIds;

        int quizzesCreated = seedQuizzes(allTopicNames, allTopicIds);
        int globalRankingEntriesUpserted = seedMyRanking(user, rankingTopicIds);
        int streakDaysCreated = seedMyStreak(user, rankingTopicIds);

        return new MyTestDataSeedResult(
                user.getKakaoId(),
                user.getNickname(),
                rankingTopicIds.size(),
                quizzesCreated,
                globalRankingEntriesUpserted,
                rankingTopicIds.size(),
                streakDaysCreated
        );
    }

    private List<String> seedTopicNames() {
        return List.of(
                "Network",
                "OS",
                "Database",
                "Java",
                "Spring",
                "Algorithm"
        );
    }

    private SeedTopicsResult seedTopics(List<String> topicNames) {
        List<TopicJpaEntity> existing = topicRepository.findByNameIn(topicNames);
        Map<String, TopicJpaEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(TopicJpaEntity::getName, topic -> topic));

        List<TopicJpaEntity> toCreate = topicNames.stream()
                .filter(name -> !existingMap.containsKey(name))
                .map(name -> TopicJpaEntity.builder().name(name).build())
                .toList();

        int created = 0;
        if (!toCreate.isEmpty()) {
            topicRepository.saveAll(toCreate);
            toCreate.forEach(topic -> existingMap.put(topic.getName(), topic));
            created = toCreate.size();
        }

        return new SeedTopicsResult(existingMap, created);
    }

    private List<SeedUser> seedUsers() {
        return List.of(
                new SeedUser(1001L, "user01", QuizLevel.JUNIOR, List.of(0, 1)),
                new SeedUser(1002L, "user02", QuizLevel.JUNIOR, List.of(2, 3)),
                new SeedUser(1003L, "user03", QuizLevel.SEMIPRO, List.of(1, 4)),
                new SeedUser(1004L, "user04", QuizLevel.SEMIPRO, List.of(0, 5)),
                new SeedUser(1005L, "user05", QuizLevel.PRO, List.of(3, 4, 5)),
                new SeedUser(1006L, "user06", QuizLevel.PRO, List.of(0, 2, 5))
        );
    }

    private int seedUsers(List<SeedUserResolved> users) {
        int created = 0;
        for (SeedUserResolved seedUser : users) {
            boolean existsByKakaoId = userRepository.existsByKakaoId(seedUser.kakaoId());
            if (existsByKakaoId) {
                continue;
            }

            User user = User.builder()
                    .kakaoId(seedUser.kakaoId())
                    .kakaoEmail(seedUser.nickname() + "@example.com")
                    .nickname(seedUser.nickname())
                    .profileImage("https://picsum.photos/seed/" + seedUser.nickname() + "/200/200")
                    .quizLevel(seedUser.quizLevel())
                    .interestTopicIds(seedUser.interestTopicIds())
                    .build();

            UserJpaEntity entity = UserJpaEntity.from(user);
            userRepository.save(entity);
            created++;
        }
        return created;
    }

    private int seedQuizzes(List<String> topicNames, List<Long> topicIds) {
        List<QuizJpaEntity> quizzes = new ArrayList<>();
        for (int i = 0; i < topicNames.size(); i++) {
            String topicName = topicNames.get(i);
            Long topicId = topicIds.get(i);
            quizzes.addAll(buildMissingQuizzesForTopic(topicName, topicId));
        }

        if (quizzes.isEmpty()) {
            return 0;
        }

        quizRepository.saveAll(quizzes);
        return quizzes.size();
    }

    private List<QuizJpaEntity> buildMissingQuizzesForTopic(String topicName, Long topicId) {
        List<QuizJpaEntity> quizzes = new ArrayList<>();

        for (QuizLevel level : QuizLevel.values()) {
            for (QuizType type : QuizType.values()) {
                int existingCount = (int) quizRepository.countDevSeedByLevelAndTypeAndTopicId(
                        level,
                        type,
                        topicId,
                        DEV_SEED_PREFIX
                );
                int missingCount = Math.max(0, QUIZZES_PER_TYPE_AND_LEVEL - existingCount);

                for (int variant = 1; variant <= missingCount; variant++) {
                    quizzes.add(buildQuiz(topicName, topicId, level, type, existingCount + variant));
                }
            }
        }

        return quizzes;
    }

    private QuizJpaEntity buildQuiz(String topicName, Long topicId, QuizLevel level, QuizType type, int sequence) {
        return switch (type) {
            case OX -> buildOxQuiz(topicName, topicId, level, sequence);
            case MULTIPLE_CHOICE -> buildMultipleChoiceQuiz(topicName, topicId, level, sequence);
            case SHORT_ANSWER -> buildShortAnswerQuiz(topicName, topicId, level, sequence);
            case VOICE -> buildVoiceQuiz(topicName, topicId, level, sequence);
        };
    }

    private QuizOxJpaEntity buildOxQuiz(String topicName, Long topicId, QuizLevel level, int sequence) {
        boolean answer = sequence % 2 == 1;
        String levelLabel = level.name();
        return QuizOxJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content("[DEV-SEED][" + topicName + "][" + levelLabel + "][OX][" + sequence + "] "
                        + topicName + " statement " + sequence + " is "
                        + (answer ? "correct" : "incorrect") + ".")
                .answer(answer)
                .explain(topicName + " " + levelLabel + " OX explanation " + sequence + ".")
                .build();
    }

    private QuizMultipleChoiceJpaEntity buildMultipleChoiceQuiz(String topicName, Long topicId, QuizLevel level, int sequence) {
        int answer = (sequence % 4) + 1;
        String levelLabel = level.name();
        return QuizMultipleChoiceJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content("[DEV-SEED][" + topicName + "][" + levelLabel + "][MULTIPLE_CHOICE][" + sequence + "] "
                        + topicName + " multiple choice question " + sequence + ".")
                .answer(answer)
                .choice(buildChoices(topicName, levelLabel, sequence))
                .explain(topicName + " " + levelLabel + " multiple choice explanation " + sequence + ".")
                .build();
    }

    private QuizShortAnswerJpaEntity buildShortAnswerQuiz(String topicName, Long topicId, QuizLevel level, int sequence) {
        String levelLabel = level.name();
        return QuizShortAnswerJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content("[DEV-SEED][" + topicName + "][" + levelLabel + "][SHORT_ANSWER][" + sequence + "] "
                        + topicName + " short answer question " + sequence + ".")
                .answer(topicName + " " + levelLabel + " keyword " + sequence)
                .explain(topicName + " " + levelLabel + " short answer explanation " + sequence + ".")
                .build();
    }

    private QuizVoiceJpaEntity buildVoiceQuiz(String topicName, Long topicId, QuizLevel level, int sequence) {
        String voiceKey = topicName.toLowerCase(Locale.ROOT).replace(" ", "-");
        String levelLabel = level.name().toLowerCase(Locale.ROOT);
        return QuizVoiceJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content("voice://dev-seed/" + voiceKey + "/" + levelLabel + "/question-" + sequence)
                .contentText("[DEV-SEED][" + topicName + "][" + level.name() + "][VOICE][" + sequence + "] "
                        + topicName + " voice question " + sequence + ".")
                .answer(topicName + " " + level.name() + " voice answer " + sequence)
                .explain(topicName + " " + level.name() + " voice explanation " + sequence + ".")
                .build();
    }

    private String buildChoices(String topicName, String levelLabel, int sequence) {
        return topicName + " " + levelLabel + " choice " + sequence + "-1"
                + "||" + topicName + " " + levelLabel + " choice " + sequence + "-2"
                + "||" + topicName + " " + levelLabel + " choice " + sequence + "-3"
                + "||" + topicName + " " + levelLabel + " choice " + sequence + "-4";
    }

    private int seedRanking(List<SeedUserResolved> users) {
        Long existing = stringRedisTemplate.opsForZSet().zCard(GLOBAL_RANKING_KEY);
        if (existing != null && existing > 0) {
            return 0;
        }

        int created = 0;
        int baseScore = 320;
        int step = 30;

        for (int i = 0; i < users.size(); i++) {
            SeedUserResolved user = users.get(i);
            long score = baseScore - (long) (i * step);
            stringRedisTemplate.opsForZSet()
                    .add(GLOBAL_RANKING_KEY, String.valueOf(user.kakaoId()), score);
            created++;

            for (Long topicId : user.interestTopicIds()) {
                String key = TOPIC_RANKING_KEY_PREFIX + topicId;
                long topicScore = Math.max(10, score / 2);
                stringRedisTemplate.opsForZSet()
                        .add(key, String.valueOf(user.kakaoId()), topicScore);
            }
        }
        return created;
    }

    private int seedStreaks(List<SeedUserResolved> users) {
        int created = 0;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < users.size(); i++) {
            SeedUserResolved user = users.get(i);
            Optional<UserStreakMongoEntity> existing = userStreakRepository.findByUserId(user.kakaoId());
            if (existing.isPresent()) {
                continue;
            }

            Map<String, DailyStreakRecordMongoValue> records = new HashMap<>();
            int currentStreak = 0;
            int longestStreak = 0;
            int totalSolved = 0;

            for (int day = 6; day >= 0; day--) {
                LocalDate date = today.minusDays(day);
                boolean solved = day % 2 == 0;
                int quizCount = solved ? 3 + (i % 3) : 0;
                if (solved) {
                    currentStreak++;
                    totalSolved += quizCount;
                } else {
                    currentStreak = 0;
                }
                longestStreak = Math.max(longestStreak, currentStreak);

                records.put(date.toString(), new DailyStreakRecordMongoValue(
                        solved,
                        quizCount,
                        currentStreak,
                        user.interestTopicIds(),
                        List.of("seed-" + user.kakaoId() + "-" + date)
                ));
            }

            UserStreakMongoEntity streak = UserStreakMongoEntity.builder()
                    .userId(user.kakaoId())
                    .currentStreak(currentStreak)
                    .longestStreak(longestStreak)
                    .totalSolved(totalSolved)
                    .dailyRecords(records)
                    .lastSolvedDate(today)
                    .build();

            userStreakRepository.save(streak);
            created++;
        }

        return created;
    }

    private int seedMyRanking(UserJpaEntity user, List<Long> topicIds) {
        long globalScore = 540L;
        stringRedisTemplate.opsForZSet()
                .add(GLOBAL_RANKING_KEY, String.valueOf(user.getKakaoId()), globalScore);

        for (int i = 0; i < topicIds.size(); i++) {
            Long topicId = topicIds.get(i);
            long topicScore = Math.max(80L, globalScore - (long) i * 17L);
            String key = TOPIC_RANKING_KEY_PREFIX + topicId;
            stringRedisTemplate.opsForZSet()
                    .add(key, String.valueOf(user.getKakaoId()), topicScore);
        }

        return 1;
    }

    private int seedMyStreak(UserJpaEntity user, List<Long> topicIds) {
        LocalDate today = LocalDate.now();
        Map<String, DailyStreakRecordMongoValue> records = new HashMap<>();
        int totalSolved = 0;

        for (int day = 27; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            boolean solved = day != 6 && day != 13 && day != 20;
            int quizCount = solved ? 2 + ((27 - day) % 4) : 0;
            totalSolved += quizCount;

            List<Long> solvedTopicIds = solved ? resolveSolvedTopicIds(topicIds, day) : List.of();
            List<String> solvedQuizIds = solved
                    ? buildSolvedQuizIds(user.getKakaoId(), date, quizCount)
                    : List.of();

            records.put(date.toString(), new DailyStreakRecordMongoValue(
                    solved,
                    quizCount,
                    0,
                    solvedTopicIds,
                    solvedQuizIds
            ));
        }

        int currentStreak = calculateTrailingStreak(records, today);
        int longestStreak = calculateLongestStreak(records);
        Map<String, DailyStreakRecordMongoValue> normalizedRecords = applyStreakSnapshots(records);
        int finalTotalSolved = totalSolved;

        UserStreakMongoEntity streak = userStreakRepository.findByUserId(user.getKakaoId())
                .map(existing -> UserStreakMongoEntity.builder()
                        .id(existing.getId())
                        .version(existing.getVersion())
                        .userId(user.getKakaoId())
                        .currentStreak(currentStreak)
                        .longestStreak(longestStreak)
                        .totalSolved(finalTotalSolved)
                        .dailyRecords(normalizedRecords)
                        .lastSolvedDate(today)
                        .build())
                .orElseGet(() -> UserStreakMongoEntity.builder()
                        .userId(user.getKakaoId())
                        .currentStreak(currentStreak)
                        .longestStreak(longestStreak)
                        .totalSolved(finalTotalSolved)
                        .dailyRecords(normalizedRecords)
                        .lastSolvedDate(today)
                        .build());

        userStreakRepository.save(streak);
        return normalizedRecords.size();
    }

    private List<Long> resolveSolvedTopicIds(List<Long> topicIds, int day) {
        if (topicIds.isEmpty()) {
            return List.of();
        }

        int firstIndex = Math.floorMod(day, topicIds.size());
        int secondIndex = Math.floorMod(day + 1, topicIds.size());
        if (firstIndex == secondIndex) {
            return List.of(topicIds.get(firstIndex));
        }
        return List.of(topicIds.get(firstIndex), topicIds.get(secondIndex));
    }

    private List<String> buildSolvedQuizIds(Long kakaoId, LocalDate date, int quizCount) {
        List<String> solvedQuizIds = new ArrayList<>();
        for (int i = 0; i < quizCount; i++) {
            solvedQuizIds.add("my-seed-" + kakaoId + "-" + date + "-" + (i + 1));
        }
        return solvedQuizIds;
    }

    private int calculateTrailingStreak(Map<String, DailyStreakRecordMongoValue> records, LocalDate today) {
        int streak = 0;
        for (int offset = 0; offset < records.size(); offset++) {
            DailyStreakRecordMongoValue record = records.get(today.minusDays(offset).toString());
            if (record == null || !record.isSolved()) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int calculateLongestStreak(Map<String, DailyStreakRecordMongoValue> records) {
        return applyStreakSnapshots(records).values().stream()
                .mapToInt(DailyStreakRecordMongoValue::getStreakAtEndOfDay)
                .max()
                .orElse(0);
    }

    private Map<String, DailyStreakRecordMongoValue> applyStreakSnapshots(Map<String, DailyStreakRecordMongoValue> records) {
        Map<String, DailyStreakRecordMongoValue> normalized = new HashMap<>();
        int currentStreak = 0;

        List<LocalDate> dates = records.keySet().stream()
                .map(LocalDate::parse)
                .sorted()
                .toList();

        for (LocalDate date : dates) {
            DailyStreakRecordMongoValue record = records.get(date.toString());
            currentStreak = record.isSolved() ? currentStreak + 1 : 0;
            normalized.put(date.toString(), new DailyStreakRecordMongoValue(
                    record.isSolved(),
                    record.getQuizCount(),
                    currentStreak,
                    record.getTopicIds(),
                    record.getAppliedGradingLogIds()
            ));
        }

        return normalized;
    }

    private record SeedUser(Long kakaoId, String nickname, QuizLevel quizLevel, List<Integer> interestIndexes) {
        SeedUserResolved resolve(List<Long> topicIds) {
            List<Long> resolved = interestIndexes.stream()
                    .filter(index -> index >= 0 && index < topicIds.size())
                    .map(topicIds::get)
                    .toList();
            return new SeedUserResolved(kakaoId, nickname, quizLevel, resolved);
        }
    }

    private record SeedUserResolved(Long kakaoId, String nickname, QuizLevel quizLevel, List<Long> interestTopicIds) {
    }

    private record SeedTopicsResult(Map<String, TopicJpaEntity> topics, int createdCount) {
    }
}
