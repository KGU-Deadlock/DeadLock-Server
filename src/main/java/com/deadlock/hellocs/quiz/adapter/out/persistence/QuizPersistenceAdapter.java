package com.deadlock.hellocs.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.application.port.out.LoadQuizPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QuizPersistenceAdapter implements LoadQuizPort {

    private final QuizRepository quizRepository;

    @Override
    public List<Quiz> findQuizzesByCriteria(QuizLevel level, List<Long> topicIds, QuizType type, int count) {
        List<QuizJpaEntity> entities = quizRepository.findAllByLevelAndTypeAndTopicIdsIn(level, type, topicIds);
        
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: Implement efficient random selection at DB level later.
        // Currently returning first 'count' elements.
        return entities.stream()
                .limit(count)
                .map(QuizJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Quiz> findAllByIds(List<Long> quizIds) {
        return quizRepository.findAllById(quizIds).stream()
                .map(QuizJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
