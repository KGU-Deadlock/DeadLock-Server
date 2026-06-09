package com.deadlock.hellocs.quiz.adapter.out.session;

import com.deadlock.hellocs.quiz.contract.QuizSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class QuizSessionRedisConfig {

    @Bean
    public RedisTemplate<String, QuizSession> quizSessionRedisTemplate(RedisConnectionFactory factory) {
        JacksonJsonRedisSerializer<QuizSession> serializer = new JacksonJsonRedisSerializer<>(QuizSession.class);

        RedisTemplate<String, QuizSession> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}
