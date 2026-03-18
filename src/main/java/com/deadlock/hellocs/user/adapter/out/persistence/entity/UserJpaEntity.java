package com.deadlock.hellocs.user.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.domain.Role;
import com.deadlock.hellocs.user.domain.User;
import com.deadlock.hellocs.global.entity.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "users")
public class UserJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nick_name", length = 15, unique = true, nullable = false)
    private String nickname;

    @Column(name = "kakao_email", length = 40)
    private String kakaoEmail;

    @Column(name = "kakao_id", length = 40)
    private Long kakaoId;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_level")
    private QuizLevel quizLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @OneToMany(mappedBy = "userJpaEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserInterestJpaEntity> interests = new ArrayList<>();

    public User toDomain() {
        return User.builder()
                .id(this.id)
                .nickname(this.nickname)
                .kakaoEmail(this.kakaoEmail)
                .kakaoId(this.kakaoId)
                .profileImage(this.profileImage)
                .quizLevel(this.quizLevel)
                .role(this.role)
                .interestTopicIds(this.interests.stream()
                        .map(UserInterestJpaEntity::getTopicId)
                        .collect(Collectors.toList()))
                .build();
    }

    public static UserJpaEntity from(User user) {
        UserJpaEntity userJpaEntity = UserJpaEntity.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .kakaoEmail(user.getKakaoEmail())
                .kakaoId(user.getKakaoId())
                .profileImage(user.getProfileImage())
                .quizLevel(user.getQuizLevel())
                .role(user.getRole())
                .interests(new ArrayList<>())
                .build();

        if (user.getInterestTopicIds() != null) {
            user.getInterestTopicIds().forEach(topicId -> {
                UserInterestJpaEntity interest = UserInterestJpaEntity.builder()
                        .userJpaEntity(userJpaEntity)
                        .topicId(topicId)
                        .build();
                userJpaEntity.getInterests().add(interest);
            });
        }

        return userJpaEntity;
    }
}
