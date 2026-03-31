package com.deadlock.hellocs.user.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class UserControllerSpecTest {

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

    private static final String KAKAO_ID = "12345";

    // ==================== API 1: POST /v1/users ====================

    @Nested
    @DisplayName("API 1: POST /v1/users — 회원가입")
    class CreateUser {

        @Test
        @DisplayName("USR-1-01: 유효한 데이터로 회원가입 성공")
        void success() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트유저",
                                      "kakaoEmail": "test@kakao.com",
                                      "profileImage": "https://img.url/pic.jpg",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1, 2]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(createUserUseCase).should().createUser(eq(12345L), any(UserSignUpCommand.class));
        }

        @Test
        @DisplayName("USR-1-02: nickname 12자 초과 시 400 반환")
        void nicknameTooLong_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "가나다라마바사아자차카타파하",
                                      "kakaoEmail": "test@kakao.com",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-03: nickname 빈 문자열 시 400 반환")
        void nicknameBlank_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "",
                                      "kakaoEmail": "test@kakao.com",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-04: kakaoEmail 이메일 형식 아닐 때 400 반환")
        void invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "not-an-email",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-05: kakaoEmail 40자 초과 시 400 반환")
        void emailTooLong_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "verylongemailaddresstest1234567@kakao.com",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-06: profileImage 500자 초과 시 400 반환")
        void profileImageTooLong_returns400() throws Exception {
            String longUrl = "https://img.url/" + "a".repeat(485);
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "test@kakao.com",
                                      "profileImage": "%s",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """.formatted(longUrl)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-07: quizLevel null 시 400 반환")
        void quizLevelNull_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "test@kakao.com",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-08: interests 빈 리스트 시 400 반환")
        void interestsEmpty_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "test@kakao.com",
                                      "quizLevel": "JUNIOR",
                                      "interests": []
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-09: interests에 null 항목 포함 시 400 반환")
        void interestsContainsNull_returns400() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "kakaoEmail": "test@kakao.com",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1, null]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-1-10: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "테스트",
                                      "quizLevel": "JUNIOR",
                                      "interests": [1]
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(createUserUseCase).shouldHaveNoInteractions();
        }

        // TODO: USR-1-11 — 만료된 JWT 토큰 테스트는 @WebMvcTest에서 시뮬레이션 불가 (스킵)
        // TODO: USR-1-12 — 존재하지 않는 topicId 검증 로직이 서비스에 미구현 (보류)
    }

    // ==================== API 2: GET /v1/users/me ====================

    @Nested
    @DisplayName("API 2: GET /v1/users/me — 내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("USR-2-01: 인증된 사용자 프로필 조회 성공")
        void success() throws Exception {
            given(loadUserUseCase.getProfile(12345L))
                    .willReturn(new ProfileResult(
                            "https://img.url/pic.jpg",
                            "테스트유저",
                            List.of("OS", "DB")
                    ));

            mockMvc.perform(get("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.nickname").value("테스트유저"))
                    .andExpect(jsonPath("$.data.profileImage").value("https://img.url/pic.jpg"))
                    .andExpect(jsonPath("$.data.interests[0]").value("OS"))
                    .andExpect(jsonPath("$.data.interests[1]").value("DB"));

            then(loadUserUseCase).should().getProfile(12345L);
        }

        @Test
        @DisplayName("USR-2-02: 미가입 사용자(JWT 유효하나 DB에 없음) 조회 시 401 반환")
        void unregisteredUser_returns401() throws Exception {
            given(loadUserUseCase.getProfile(12345L))
                    .willThrow(new CustomException(ErrorStatus._USER_NOT_FOUND));

            mockMvc.perform(get("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("USER401"));

            then(loadUserUseCase).should().getProfile(12345L);
        }

        @Test
        @DisplayName("USR-2-03: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(loadUserUseCase).shouldHaveNoInteractions();
        }
    }

    // ==================== API 3: PATCH /v1/users/me ====================

    @Nested
    @DisplayName("API 3: PATCH /v1/users/me — 프로필 수정")
    class UpdateMyProfile {

        @Test
        @DisplayName("USR-3-01: nickname만 수정 성공")
        void nicknameOnly_success() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "새닉네임"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().updateMyInfo(eq(12345L),
                    eq(new UpdateMyInfoCommand("새닉네임", null, null)));
        }

        @Test
        @DisplayName("USR-3-02: profileImage만 수정 성공")
        void profileImageOnly_success() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileImage": "https://new-img.url/pic.jpg"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().updateMyInfo(eq(12345L),
                    eq(new UpdateMyInfoCommand(null, "https://new-img.url/pic.jpg", null)));
        }

        @Test
        @DisplayName("USR-3-03: interestTopicIds만 수정 성공")
        void interestTopicIdsOnly_success() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"interestTopicIds": [2, 3]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().updateMyInfo(eq(12345L),
                    eq(new UpdateMyInfoCommand(null, null, List.of(2L, 3L))));
        }

        @Test
        @DisplayName("USR-3-04: 모든 필드 동시 수정 성공")
        void allFields_success() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nickname": "새닉네임",
                                      "profileImage": "https://new.url/pic.jpg",
                                      "interestTopicIds": [1, 3]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().updateMyInfo(eq(12345L),
                    eq(new UpdateMyInfoCommand("새닉네임", "https://new.url/pic.jpg", List.of(1L, 3L))));
        }

        @Test
        @DisplayName("USR-3-05: nickname 12자 초과 시 400 반환")
        void nicknameTooLong_returns400() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "가나다라마바사아자차카타파하"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-3-06: nickname 빈 문자열(공백) 시 400 반환")
        void nicknameBlank_returns400() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "   "}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-3-07: profileImage 500자 초과 시 400 반환")
        void profileImageTooLong_returns400() throws Exception {
            String longUrl = "https://img.url/" + "a".repeat(485);
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileImage": "%s"}
                                    """.formatted(longUrl)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-3-09: interestTopicIds 빈 리스트 시 400 반환")
        void interestTopicIdsEmpty_returns400() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"interestTopicIds": []}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-3-10: interestTopicIds에 null 포함 시 400 반환")
        void interestTopicIdsContainsNull_returns400() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"interestTopicIds": [1, null]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-3-12: JsonAlias(name/userName/username)로 닉네임 수정 성공")
        void jsonAlias_success() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "별칭테스트"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().updateMyInfo(eq(12345L),
                    eq(new UpdateMyInfoCommand("별칭테스트", null, null)));
        }

        @Test
        @DisplayName("USR-3-13: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(patch("/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "새닉네임"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        // TODO: USR-3-08 — profileImage 빈 문자열 테스트 (명세 데이터 불완전, 스킵)
        // TODO: USR-3-11 — 존재하지 않는 topicId 검증 로직이 서비스에 미구현 (보류)
    }

    // ==================== API 4: DELETE /v1/users/me ====================

    @Nested
    @DisplayName("API 4: DELETE /v1/users/me — 회원 탈퇴")
    class DeleteMyAccount {

        @Test
        @DisplayName("USR-4-01: 인증된 사용자 탈퇴 성공")
        void success() throws Exception {
            mockMvc.perform(delete("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(manageUserUseCase).should().deleteMyAccount(12345L);
        }

        @Test
        @DisplayName("USR-4-02: 탈퇴 후 동일 사용자로 프로필 조회 시 401 반환")
        void thenGetProfileFails() throws Exception {
            willDoNothing().given(manageUserUseCase).deleteMyAccount(12345L);

            mockMvc.perform(delete("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            given(loadUserUseCase.getProfile(12345L))
                    .willThrow(new CustomException(ErrorStatus._USER_NOT_FOUND));

            mockMvc.perform(get("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("USER401"));
        }

        @Test
        @DisplayName("USR-4-03: 이미 탈퇴한 사용자가 다시 탈퇴 요청 시 401 반환")
        void alreadyDeletedUser_returns401() throws Exception {
            willThrow(new CustomException(ErrorStatus._USER_NOT_FOUND))
                    .given(manageUserUseCase).deleteMyAccount(12345L);

            mockMvc.perform(delete("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("USER401"));
        }

        @Test
        @DisplayName("USR-4-04: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(delete("/v1/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(manageUserUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("USR-4-05: 미가입 사용자(JWT 유효하나 DB에 없음) 탈퇴 요청 시 401 반환")
        void unregisteredUser_returns401() throws Exception {
            willThrow(new CustomException(ErrorStatus._USER_NOT_FOUND))
                    .given(manageUserUseCase).deleteMyAccount(12345L);

            mockMvc.perform(delete("/v1/users/me")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("USER401"));
        }
    }
}
