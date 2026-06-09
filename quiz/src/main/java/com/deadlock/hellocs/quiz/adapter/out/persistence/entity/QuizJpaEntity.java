package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.common.jpa.BaseJpaEntity;
import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz JPA Entity
 * 
 * Joined 전략으로 상속 구현
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "quiz")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
public abstract class QuizJpaEntity extends BaseJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private QuizLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    private QuizType type;
    
    @ElementCollection
    @CollectionTable(
            name = "quiz_category",
            joinColumns = @JoinColumn(name = "quiz_id")
    )
    @Column(name = "topic_id")
    @lombok.Builder.Default
    private List<Long> topicIds = new ArrayList<>();
    
    /**
     * Entity → Domain 변환
     */
    public abstract Quiz toDomain();

    /**
     * 채점/시딩에서 사용하는 정답 문자열 표현.
     * 도메인의 {@code getAnswerAsString()}과 동일한 포맷을 반환한다.
     * (OX → "true"/"false", MULTIPLE_CHOICE → 인덱스 숫자, SHORT_ANSWER·VOICE → 정답 문자열)
     */
    public abstract String correctAnswerAsString();
}
