package com.deadlock.hellocs.topic.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.topic.adapter.in.web.docs.TopicControllerDocs;
import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController implements TopicControllerDocs {

    private final LoadTopicUseCase loadTopicUseCase;

    @GetMapping
    @Override
    public ApiResponse<List<TopicResult>> getAllTopics() {
        return ApiResponse.onSuccess(loadTopicUseCase.getAllTopics());
    }
}
