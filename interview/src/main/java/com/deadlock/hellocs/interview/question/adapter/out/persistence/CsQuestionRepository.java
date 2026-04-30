package com.deadlock.hellocs.interview.question.adapter.out.persistence;

import com.deadlock.hellocs.interview.question.adapter.out.persistence.entity.CsQuestionJpaEntity;
import com.deadlock.hellocs.interview.question.domain.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CsQuestionRepository extends JpaRepository<CsQuestionJpaEntity, Long> {

    @Query(value = "SELECT * FROM cs_questions WHERE category = :category ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    CsQuestionJpaEntity findOneRandomByCategory(String category);

    @Query(value = "SELECT COUNT(*) FROM cs_questions", nativeQuery = true)
    long countAll();
}
