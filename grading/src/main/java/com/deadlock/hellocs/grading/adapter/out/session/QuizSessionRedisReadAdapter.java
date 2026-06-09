package com.deadlock.hellocs.grading.adapter.out.session;

import com.deadlock.hellocs.quiz.contract.QuizSession;
import com.deadlock.hellocs.quiz.contract.QuizSessionKey;
import com.deadlock.hellocs.quiz.contract.QueryQuizSessionOutputPort;
import com.deadlock.hellocs.grading.exception.GradingErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizSessionRedisReadAdapter implements QueryQuizSessionOutputPort {

    private final RedisTemplate<String, QuizSession> quizSessionRedisTemplate;

    @Override
    public QuizSession findByUserIdAndQuizIds(Long userId, List<Long> quizIds) {
        QuizSession session = quizSessionRedisTemplate.opsForValue().get(QuizSessionKey.of(userId, quizIds));
        if (session == null) {
            throw new CustomException(GradingErrorStatus.GRADING_SESSION_NOT_FOUND);
        }
        return session;
    }
}
