package com.deadlock.hellocs.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<QuizJpaEntity, Long> {

    @Query("SELECT q FROM QuizJpaEntity q JOIN q.topicIds t WHERE q.level = :level AND q.type = :type AND t IN :topicIds")
    List<QuizJpaEntity> findAllByLevelAndTypeAndTopicIdsIn(
            @Param("level") QuizLevel level,
            @Param("type") QuizType type,
            @Param("topicIds") List<Long> topicIds
    );
}
