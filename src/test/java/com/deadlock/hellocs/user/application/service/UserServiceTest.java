package com.deadlock.hellocs.user.application.service;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.adapter.in.web.dto.MyInfoResponse;
import com.deadlock.hellocs.user.adapter.in.web.dto.UpdateMyInfoRequest;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private LoadTopicPort loadTopicPort;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(loadUserPort, saveUserPort, loadTopicPort);
    }

    @Test
    void updateMyInfoUpdatesOnlyProvidedFields() {
        User user = User.builder()
                .id(1L)
                .nickname("oldNick")
                .profileImage("old-image")
                .kakaoEmail("test@kakao.com")
                .kakaoId(100L)
                .quizLevel(QuizLevel.JUNIOR)
                .interestTopicIds(List.of(1L, 2L))
                .build();

        when(loadUserPort.loadUserByKakaoId(100L)).thenReturn(user);
        when(loadUserPort.existsByNickname("newNick")).thenReturn(false);

        MyInfoResponse result = userService.updateMyInfo(100L, new UpdateMyInfoRequest("newNick", null));

        assertEquals(1L, result.id());
        assertEquals("newNick", result.nickname());
        assertEquals("old-image", result.profileImage());
        verify(saveUserPort).saveUser(user);
    }

    @Test
    void updateMyInfoThrowsWhenNicknameAlreadyExists() {
        User user = User.builder()
                .id(1L)
                .nickname("oldNick")
                .profileImage("old-image")
                .kakaoEmail("test@kakao.com")
                .kakaoId(100L)
                .quizLevel(QuizLevel.JUNIOR)
                .interestTopicIds(List.of(1L, 2L))
                .build();

        when(loadUserPort.loadUserByKakaoId(100L)).thenReturn(user);
        when(loadUserPort.existsByNickname("takenNick")).thenReturn(true);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> userService.updateMyInfo(100L, new UpdateMyInfoRequest("takenNick", "new-image"))
        );

        assertEquals(ErrorStatus._NICKNAME_ALREADY_EXISTS, exception.getErrorCode());
        verify(saveUserPort, never()).saveUser(user);
    }

    @Test
    void deleteMyAccountDelegatesToPort() {
        userService.deleteMyAccount(200L);

        verify(saveUserPort).deleteUserByKakaoId(200L);
    }

    @Test
    void isExistReturnsTrueWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .nickname("oldNick")
                .kakaoId(100L)
                .quizLevel(QuizLevel.JUNIOR)
                .build();

        when(loadUserPort.loadUserByKakaoId(100L)).thenReturn(user);

        assertTrue(userService.isExist(100L));
    }

    @Test
    void isExistReturnsFalseWhenUserDoesNotExist() {
        when(loadUserPort.loadUserByKakaoId(100L))
                .thenThrow(new CustomException(ErrorStatus._USER_NOT_FOUND));

        assertFalse(userService.isExist(100L));
    }
}
