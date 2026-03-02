package com.deadlock.hellocs.dev;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev")
public class TestDataController {

    private final TestDataService testDataService;

    @PostMapping("/seed")
    public ApiResponse<SeedResult> seed() {
        return ApiResponse.onSuccess(testDataService.seed());
    }
}
