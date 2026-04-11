package com.deadlock.hellocs.global.internal;

import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ranking-streak-service → core-service 서비스 간 내부 API (토픽 이름 조회).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/topics")
public class InternalTopicController {

    private final LoadTopicUseCase loadTopicUseCase;

    @GetMapping("/names")
    public List<String> getTopicNames(@RequestParam List<Long> topicIds) {
        return loadTopicUseCase.getTopicNames(topicIds);
    }

    @GetMapping("/name-map")
    public Map<Long, String> getTopicNameMap(@RequestParam List<Long> topicIds) {
        return loadTopicUseCase.getTopicNameMap(topicIds);
    }
}
