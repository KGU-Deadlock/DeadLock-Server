package com.deadlock.hellocs.user.domain;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
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
    private Role role = Role.USER;
    @Builder.Default
    private List<Long> interestTopicIds = new ArrayList<>();

    public static User createUser(Long kakaoId,
                                  String kakaoEmail,
                                  String nickname,
                                  String profileImage,
                                  QuizLevel quizLevel,
                                  List<Long> interestTopicIds) {
        return User.builder()
                .kakaoId(kakaoId)
                .kakaoEmail(kakaoEmail)
                .nickname(nickname)
                .profileImage(profileImage)
                .quizLevel(quizLevel)
                .role(Role.USER)
                .interestTopicIds(interestTopicIds == null ? new ArrayList<>() : new ArrayList<>(interestTopicIds))
                .build();
    }

    public void update(UpdateMyInfoCommand command) {
        if (command.nickname() != null) {
            this.nickname = command.nickname();
        }
        if (command.profileImage() != null) {
            this.profileImage = command.profileImage();
        }
        if (command.interestTopicIds() != null) {
            this.interestTopicIds = new ArrayList<>(command.interestTopicIds());
        }
    }
}
