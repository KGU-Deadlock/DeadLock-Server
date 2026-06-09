package com.deadlock.hellocs.grading.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.grading.application.port.in.QuerySolvedQuizIdsInputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/v1/gradings")
@RequiredArgsConstructor
public class SolvedQuizIdsController {

    private final QuerySolvedQuizIdsInputPort querySolvedQuizIdsInputPort;

    @GetMapping("/users/{userId}/solved-quiz-ids")
    public ApiResponse<Set<Long>> getSolvedQuizIds(@PathVariable Long userId) {
        return ApiResponse.onSuccess(querySolvedQuizIdsInputPort.getSolvedQuizIds(userId));
    }
}
