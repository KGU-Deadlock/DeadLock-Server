package com.deadlock.hellocs.quiz.adapter.out.persistence;

import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizType;
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
    // TODO: QueryDSL 이용이나 기타 방법...
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

    /**
     * DEV-SEED 퀴즈 전체 조회 (답안키 제공용).
     * VOICE는 contentText 기준으로 prefix를 확인하므로 TYPE/TREAT 패턴 사용.
     */
    @Query("""
           SELECT DISTINCT q FROM QuizJpaEntity q
           WHERE (
                (TYPE(q) = QuizOxJpaEntity AND TREAT(q AS QuizOxJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizMultipleChoiceJpaEntity AND TREAT(q AS QuizMultipleChoiceJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizShortAnswerJpaEntity AND TREAT(q AS QuizShortAnswerJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizVoiceJpaEntity AND TREAT(q AS QuizVoiceJpaEntity).contentText LIKE CONCAT(:prefix, '%'))
           )
           """)
    List<QuizJpaEntity> findAllDevSeed(@Param("prefix") String prefix);

    @Query("""
           SELECT COUNT(DISTINCT q) FROM QuizJpaEntity q
           JOIN q.topicIds t
           WHERE q.level = :level
           AND q.type = :type
           AND t = :topicId
           AND (
                (TYPE(q) = QuizOxJpaEntity AND TREAT(q AS QuizOxJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizMultipleChoiceJpaEntity AND TREAT(q AS QuizMultipleChoiceJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizShortAnswerJpaEntity AND TREAT(q AS QuizShortAnswerJpaEntity).content LIKE CONCAT(:prefix, '%'))
                OR (TYPE(q) = QuizVoiceJpaEntity AND TREAT(q AS QuizVoiceJpaEntity).contentText LIKE CONCAT(:prefix, '%'))
           )
           """)
    long countDevSeedByLevelAndTypeAndTopicId(
            @Param("level") QuizLevel level,
            @Param("type") QuizType type,
            @Param("topicId") Long topicId,
            @Param("prefix") String prefix
    );
}
