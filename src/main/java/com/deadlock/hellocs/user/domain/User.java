package com.deadlock.hellocs.user.domain;

import com.deadlock.hellocs.quiz.QuizLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class User {
    private final Long id;
    private String nickname;
    private String kakaoEmail;
    private Long kakaoId;
    private String profileImage;
    private QuizLevel quizLevel;
    @Builder.Default
    private List<Long> interestTopicIds = new ArrayList<>();

    public static User createUser(Long kakaoId, String kakaoEmail, String nickname, QuizLevel quizLevel) {
        return User.builder()
                .kakaoId(kakaoId)
                .kakaoEmail(kakaoEmail)
                .nickname(nickname)
                .quizLevel(quizLevel)
                .build();
    }

    public void updateProfile(String nickname, String profileImage, List<Long> interestTopicIds) {
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.interestTopicIds = interestTopicIds;
    }
}
