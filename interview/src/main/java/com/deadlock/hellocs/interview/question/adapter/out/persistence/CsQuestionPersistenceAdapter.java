package com.deadlock.hellocs.interview.question.adapter.out.persistence;

import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.interview.exception.InterviewErrorStatus;
import com.deadlock.hellocs.interview.question.application.port.out.QueryCsQuestionOutputPort;
import com.deadlock.hellocs.interview.question.domain.CsQuestion;
import com.deadlock.hellocs.interview.question.domain.QuestionCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CsQuestionPersistenceAdapter implements QueryCsQuestionOutputPort {

    private final CsQuestionRepository csQuestionRepository;

    // OS 1개 + Computer Architecture 1개로 카테고리 균형 추출
    @Override
    public List<CsQuestion> findRandomTwoWithoutDuplicate() {
        if (csQuestionRepository.countAll() < 2) {
            throw new CustomException(InterviewErrorStatus.INTERVIEW_CS_QUESTION_NOT_ENOUGH);
        }

        CsQuestion osQuestion = csQuestionRepository
                .findOneRandomByCategory(QuestionCategory.OS.name())
                .toDomain();

        CsQuestion caQuestion = csQuestionRepository
                .findOneRandomByCategory(QuestionCategory.COMPUTER_ARCHITECTURE.name())
                .toDomain();

        return List.of(osQuestion, caQuestion);
    }
}
