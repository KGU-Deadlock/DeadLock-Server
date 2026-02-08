package com.deadlock.hellocs.quiz.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<QuizJpaEntity, Long> {
    
    /**
     * 조건에 맞는 Quiz 조회
     */
    @Query("SELECT DISTINCT q FROM QuizJpaEntity q " +
           "JOIN q.topicIds t " +
           "WHERE q.level = :level " +
           "AND q.type = :type " +
           "AND t IN :topicIds")
    List<QuizJpaEntity> findByLevelAndTypeAndTopicIds(
            @Param("level") QuizLevel level,
            @Param("type") QuizType type,
            @Param("topicIds") List<Long> topicIds
    );
}
