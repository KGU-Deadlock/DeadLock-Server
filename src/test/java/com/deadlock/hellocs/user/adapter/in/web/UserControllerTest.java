package com.deadlock.hellocs.user.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private LoadUserUseCase loadUserUseCase;

    @MockitoBean
    private ManageUserUseCase manageUserUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /v1/users - 인증된 사용자는 회원가입할 수 있다")
    void createUser_success() throws Exception {
        UserSignUpCommand command = new UserSignUpCommand(
                "deadlock",
                "deadlock@example.com",
                "https://cdn.example.com/profiles/me.png",
                QuizLevel.PRO,
                List.of(1L, 2L)
        );

        mockMvc.perform(post("/v1/users")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "deadlock",
                                  "kakaoEmail": "deadlock@example.com",
                                  "profileImage": "https://cdn.example.com/profiles/me.png",
                                  "quizLevel": "PRO",
                                  "interests": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(createUserUseCase).should().createUser(12345L, command);
    }

    @Test
    @DisplayName("POST /v1/users - 잘못된 회원가입 요청이면 400을 반환한다")
    void createUser_invalidRequest() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "aaaaaaaaaaaaa",
                                  "kakaoEmail": "not-an-email",
                                  "profileImage": "https://cdn.example.com/profiles/me.png",
                                  "quizLevel": null,
                                  "interests": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));

        then(createUserUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /v1/users/me - 내 프로필을 조회할 수 있다")
    void getMyProfile_success() throws Exception {
        given(loadUserUseCase.getProfile(12345L))
                .willReturn(new ProfileResult(
                        "https://cdn.example.com/profiles/me.png",
                        "deadlock",
                        List.of("OS", "DB")
                ));

        mockMvc.perform(get("/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.nickname").value("deadlock"))
                .andExpect(jsonPath("$.data.interests[0]").value("OS"));

        then(loadUserUseCase).should().getProfile(12345L);
    }

    @Test
    @DisplayName("PATCH /v1/users/me - 내 프로필을 수정할 수 있다")
    void updateMyProfile_success() throws Exception {
        UpdateMyInfoCommand command = new UpdateMyInfoCommand(
                "updated",
                "https://cdn.example.com/profiles/updated.png",
                List.of(3L, 4L)
        );

        mockMvc.perform(patch("/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "updated",
                                  "profileImage": "https://cdn.example.com/profiles/updated.png",
                                  "interestTopicIds": [3, 4]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(manageUserUseCase).should().updateMyInfo(12345L, command);
    }

    @Test
    @DisplayName("PATCH /v1/users/me - 잘못된 수정 요청이면 400을 반환한다")
    void updateMyProfile_invalidRequest() throws Exception {
        mockMvc.perform(patch("/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": " ",
                                  "profileImage": "",
                                  "interestTopicIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));

        then(manageUserUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DELETE /v1/users/me - 내 계정을 삭제할 수 있다")
    void deleteMyAccount_success() throws Exception {
        mockMvc.perform(delete("/v1/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(manageUserUseCase).should().deleteMyAccount(12345L);
    }

    @Test
    @DisplayName("GET /v1/users/me - 인증되지 않은 요청이면 401을 반환한다")
    void getMyProfile_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"))
                .andExpect(jsonPath("$.data").value((Object) null));

        then(loadUserUseCase).shouldHaveNoInteractions();
    }
}
