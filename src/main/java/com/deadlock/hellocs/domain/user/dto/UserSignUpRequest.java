package com.deadlock.hellocs.domain.user.dto;

import com.deadlock.hellocs.domain.quiz.domain.QuizLevel;

import java.util.List;

public record UserSignUpRequest(String nickname, String kakaoEmail, String profileImage, QuizLevel quizLevel, List<String> interests) {}
