package com.deadlock.hellocs.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<QuizJpaEntity, Long> {
}
