package com.deadlock.hellocs.quiz.quiz.adapter.out.session;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.quiz.application.port.out.CommandQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.QuizSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class QuizSessionRedisAdapter implements CommandQuizSessionOutputPort, QueryQuizSessionOutputPort {

    private static final String KEY_PREFIX = "quiz:session:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(QuizSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(session.userId()), json, TTL);
        } catch (JsonProcessingException e) {
            throw new CustomException(QuizErrorStatus.QUIZ_SESSION_SERIALIZE_FAILED);
        }
    }

    @Override
    public QuizSession findByUserId(Long userId) {
        String json = redisTemplate.opsForValue().get(key(userId));
        if (json == null) {
            throw new CustomException(QuizErrorStatus.QUIZ_SESSION_NOT_FOUND);
        }
        try {
            return objectMapper.readValue(json, QuizSession.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(QuizErrorStatus.QUIZ_SESSION_SERIALIZE_FAILED);
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
