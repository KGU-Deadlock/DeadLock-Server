package com.deadlock.hellocs.quiz.adapter.out.session;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.application.port.out.CommandQuizSessionOutputPort;
import com.deadlock.hellocs.quiz.contract.QuizSession;
import com.deadlock.hellocs.quiz.contract.QuizSessionKey;
import com.deadlock.hellocs.quiz.contract.QueryQuizSessionOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizSessionRedisAdapter implements CommandQuizSessionOutputPort, QueryQuizSessionOutputPort {

    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, QuizSession> quizSessionRedisTemplate;

    @Override
    public void save(QuizSession session) {
        List<Long> quizIds = new ArrayList<>(session.quizzes().keySet());
        quizSessionRedisTemplate.opsForValue().set(key(session.userId(), quizIds), session, TTL);
    }

    @Override
    public QuizSession findByUserIdAndQuizIds(Long userId, List<Long> quizIds) {
        QuizSession session = quizSessionRedisTemplate.opsForValue().get(key(userId, quizIds));
        if (session == null) {
            throw new CustomException(QuizErrorStatus.QUIZ_SESSION_NOT_FOUND);
        }
        return session;
    }

    private String key(Long userId, List<Long> quizIds) {
        return QuizSessionKey.of(userId, quizIds);
    }
}
