package com.deadlock.hellocs.dev;

import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.QuizRepository;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizMultipleChoiceJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizOxJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizShortAnswerJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizVoiceJpaEntity;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.streak.adapter.out.persistence.UserStreakRepository;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoValue;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import com.deadlock.hellocs.topic.adapter.out.persistence.TopicRepository;
import com.deadlock.hellocs.topic.adapter.out.persistence.entity.TopicJpaEntity;
import com.deadlock.hellocs.user.adapter.out.persistence.UserRepository;
import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaJpaEntity;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final String GLOBAL_RANKING_KEY = "ranking:global";
    private static final String TOPIC_RANKING_KEY_PREFIX = "ranking:topic:";

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
            boolean existsByKakaoId = userRepository.findByKakaoId(seedUser.kakaoId()).isPresent();
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

            UserJpaJpaEntity entity = UserJpaJpaEntity.from(user);
            userRepository.save(entity);
            created++;
        }
        return created;
    }

    private int seedQuizzes(List<String> topicNames, List<Long> topicIds) {
        if (quizRepository.count() > 0) {
            return 0;
        }

        List<QuizJpaEntity> quizzes = new ArrayList<>();
        for (int i = 0; i < topicNames.size(); i++) {
            String topicName = topicNames.get(i);
            Long topicId = topicIds.get(i);
            QuizLevel level = resolveLevel(i);

            quizzes.add(QuizOxJpaEntity.builder()
                    .level(level)
                    .topicIds(List.of(topicId))
                    .content("[" + topicName + "] OX: " + topicName + " is a core CS topic.")
                    .answer(true)
                    .explain("Foundational concepts in " + topicName + " appear in many CS interviews.")
                    .build());

            quizzes.add(QuizMultipleChoiceJpaEntity.builder()
                    .level(level)
                    .topicIds(List.of(topicId))
                    .content("[" + topicName + "] Which concept is most closely related?")
                    .answer(2)
                    .choice(topicName + " option 1||" + topicName + " option 2||" + topicName + " option 3||" + topicName + " option 4")
                    .explain("Answer 2 is typically associated with " + topicName + ".")
                    .build());

            quizzes.add(QuizShortAnswerJpaEntity.builder()
                    .level(level)
                    .topicIds(List.of(topicId))
                    .content("[" + topicName + "] Provide a short definition.")
                    .answer(topicName + " basics")
                    .explain("Use one sentence with a key idea.")
                    .build());

            String voiceKey = topicName.toLowerCase(Locale.ROOT).replace(" ", "-");
            quizzes.add(QuizVoiceJpaEntity.builder()
                    .level(level)
                    .topicIds(List.of(topicId))
                    .content("voice://" + voiceKey + "-question-01")
                    .contentText("[" + topicName + "] Explain this topic in one sentence.")
                    .answer("Short explanation of " + topicName)
                    .explain("Keep it concise and accurate.")
                    .build());
        }

        quizRepository.saveAll(quizzes);
        return quizzes.size();
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

    private QuizLevel resolveLevel(int index) {
        return switch (index % 3) {
            case 0 -> QuizLevel.JUNIOR;
            case 1 -> QuizLevel.SEMIPRO;
            default -> QuizLevel.PRO;
        };
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
