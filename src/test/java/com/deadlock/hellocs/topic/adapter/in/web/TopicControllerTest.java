package com.deadlock.hellocs.topic.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoadTopicUseCase loadTopicUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Nested
    @DisplayName("API 1: GET /v1/topics — 전체 토픽 조회")
    class GetAllTopics {

        @Test
        @DisplayName("TPC-1-01: 전체 토픽 목록 조회 성공")
        void getAllTopics_success() throws Exception {
            given(loadTopicUseCase.getAllTopics())
                    .willReturn(List.of(
                            new TopicResult(1L, "OS"),
                            new TopicResult(2L, "Network")
                    ));

            mockMvc.perform(get("/v1/topics")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("OS"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].name").value("Network"));

            then(loadTopicUseCase).should().getAllTopics();
        }

        @Test
        @DisplayName("TPC-1-03: 인증 없이 요청 가능 확인 (공개 API)")
        void getAllTopics_withoutAuth_success() throws Exception {
            given(loadTopicUseCase.getAllTopics())
                    .willReturn(List.of(
                            new TopicResult(1L, "OS"),
                            new TopicResult(2L, "Network")
                    ));

            mockMvc.perform(get("/v1/topics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("OS"));

            then(loadTopicUseCase).should().getAllTopics();
        }
    }
}
