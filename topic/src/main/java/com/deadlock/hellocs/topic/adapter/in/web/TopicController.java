package com.deadlock.hellocs.topic.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.topic.adapter.in.web.docs.TopicControllerDocs;
import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/topics")
@RequiredArgsConstructor
public class TopicController implements TopicControllerDocs {

    private final LoadTopicUseCase loadTopicUseCase;

    @GetMapping
    @Override
    public ApiResponse<List<TopicResult>> getAllTopics() {
        return ApiResponse.onSuccess(loadTopicUseCase.getAllTopics());
    }

    @GetMapping("/names")
    @Override
    public ApiResponse<List<String>> getTopicNames(@RequestParam List<Long> ids) {
        return ApiResponse.onSuccess(loadTopicUseCase.getTopicNames(ids));
    }

    @GetMapping("/ids")
    @Override
    public ApiResponse<List<Long>> getTopicIdsByNames(@RequestParam List<String> names) {
        return ApiResponse.onSuccess(loadTopicUseCase.getTopicIdsByNames(names));
    }
}
