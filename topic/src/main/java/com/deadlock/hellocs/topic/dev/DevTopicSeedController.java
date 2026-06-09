package com.deadlock.hellocs.topic.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.topic.adapter.out.persistence.TopicRepository;
import com.deadlock.hellocs.topic.adapter.out.persistence.entity.TopicJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * dev 서비스가 호출하는 토픽 시딩 엔드포인트. dev 프로파일에서만 활성화된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevTopicSeedController {

    private static final List<String> SEED_TOPIC_NAMES = List.of(
            "Network", "OS", "Database", "Java", "Spring", "Algorithm"
    );

    private final TopicRepository topicRepository;

    @PostMapping("/topics")
    @Transactional
    public ApiResponse<SeedTopicsResult> seedTopics() {
        List<TopicJpaEntity> existing = topicRepository.findByNameIn(SEED_TOPIC_NAMES);
        Map<String, TopicJpaEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(TopicJpaEntity::getName, t -> t));

        List<TopicJpaEntity> toCreate = SEED_TOPIC_NAMES.stream()
                .filter(name -> !existingMap.containsKey(name))
                .map(name -> TopicJpaEntity.builder().name(name).build())
                .toList();

        if (!toCreate.isEmpty()) {
            topicRepository.saveAll(toCreate);
        }

        return ApiResponse.onSuccess(new SeedTopicsResult(toCreate.size(), existing.size()));
    }

    public record SeedTopicsResult(int created, int alreadyExisted) {}
}
