package com.deadlock.hellocs.quiz.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.quiz.application.port.out.LoadQuizOutputPort;
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
public class QuizPersistenceAdapter implements LoadQuizOutputPort {
    
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
        
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        // TODO: 향후 랜덤 추출 or 정책에 따른 반환 구현
        // 현재는 앞에서부터 count개 반환
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
