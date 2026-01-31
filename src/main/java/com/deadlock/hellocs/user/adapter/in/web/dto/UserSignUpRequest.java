package com.deadlock.hellocs.user.adapter.in.web.dto;

import com.deadlock.hellocs.quiz.QuizLevel;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UserSignUpRequest(
        @Size(max = 12, message = "닉네임은 최대 12자까지 가능합니다.")
        String nickname,

        @Size(max = 40, message = "이메일은 최대 40자까지 가능합니다.")
        @Email(message = "유효한 이메일 주소를 입력해주세요.")
        String kakaoEmail,

        @Size(max = 500, message = "프로필 이미지는 최대 500자까지 가능합니다.")
        String profileImage,

        @NotNull(message = "quizLevel은 필수입니다.")
        QuizLevel quizLevel,

        @NotEmpty(message = "interests는 하나 이상 필수입니다.")
        List<String> interests
){}
