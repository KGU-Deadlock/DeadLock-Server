package com.deadlock.hellocs.quiz.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuizPersistenceAdapter implements QueryQuizOutputPort {
    
    private final QuizRepository quizRepository;
    
    @Override
    public List<Quiz> findQuizzesByCriteria(
            QuizLevel level,
            List<Long> topicIds,
            QuizType type,
            int count
    ) {
        List<QuizJpaEntity> entities = quizRepository.findByLevelAndTypeAndTopicIds(
                level, type, topicIds
        );
        // TODO: 이후 랜덤 추출 or 정책적 랜덤 반환 구현. 0개일때 대처?
        // 현재는 순서에서 count개 반환
        return entities.stream()
                .limit(count)
                .map(QuizJpaEntity::toDomain)
                .toList();
    }
    
    @Override
    public Quiz findById(Long quizId) {
        return quizRepository.findById(quizId)
                .map(QuizJpaEntity::toDomain).get();
    }
    
    @Override
    public List<Quiz> findAllByIds(List<Long> quizIds) {
        return quizRepository.findAllById(quizIds).stream()
                .map(QuizJpaEntity::toDomain)
                .toList();
    }
}
