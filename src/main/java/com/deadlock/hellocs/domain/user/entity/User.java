package com.deadlock.hellocs.domain.user.entity;

import com.deadlock.hellocs.domain.quiz.domain.QuizLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nick_name", length = 15, unique = true, nullable = false)
    private String nickname;

    @Column(name = "kakao_email", length = 40)
    private String kakaoEmail;

    @Column(name = "kakao_id", length = 40)
    private Long kakaoId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delete_at")
    private Boolean deleteAt = false;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_level")
    private QuizLevel quizLevel;
}