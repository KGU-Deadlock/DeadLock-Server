package com.deadlock.hellocs.quiz.quiz.adapter.out.session;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.quiz.application.port.out.CommandQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.QuizSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class QuizSessionRedisAdapter implements CommandQuizSessionOutputPort, QueryQuizSessionOutputPort {

    private static final String KEY_PREFIX = "quiz:session:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, QuizSession> quizSessionRedisTemplate;

    @Override
    public void save(QuizSession session) {
        quizSessionRedisTemplate.opsForValue().set(key(session.userId()), session, TTL);
    }

    @Override
    public QuizSession findByUserId(Long userId) {
        QuizSession session = quizSessionRedisTemplate.opsForValue().get(key(userId));
        if (session == null) {
            throw new CustomException(QuizErrorStatus.QUIZ_SESSION_NOT_FOUND);
        }
        return session;
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
