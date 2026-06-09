package com.deadlock.hellocs.topic.adapter.in.web.docs;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Topic", description = "주제 API")
public interface TopicControllerDocs {

    @Operation(summary = "전체 주제 목록 조회", description = "등록된 모든 주제(토픽) 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주제 목록 조회 성공")
    })
    ApiResponse<List<TopicResult>> getAllTopics();

    @Operation(summary = "[내부] ID → 이름 목록 조회", description = "내부 서비스용. 토픽 ID 목록으로 이름 목록을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토픽 이름 목록 조회 성공")
    })
    ApiResponse<List<String>> getTopicNames(@RequestParam List<Long> ids);

    @Operation(summary = "[내부] 이름 목록 → ID 조회", description = "내부 서비스용. 토픽 이름 목록으로 ID 목록을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토픽 ID 목록 조회 성공")
    })
    ApiResponse<List<Long>> getTopicIdsByNames(@RequestParam List<String> names);
}
