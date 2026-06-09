package com.deadlock.hellocs.interview.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.interview.question.adapter.out.persistence.CsQuestionRepository;
import com.deadlock.hellocs.interview.question.adapter.out.persistence.entity.CsQuestionJpaEntity;
import com.deadlock.hellocs.interview.question.domain.Difficulty;
import com.deadlock.hellocs.interview.question.domain.QuestionCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * dev 서비스가 호출하는 CS 질문 시딩 엔드포인트. dev 프로파일에서만 활성화된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevInterviewSeedController {

    private static final String DEV_SEED_PREFIX = "[DEV-SEED]";

    private final CsQuestionRepository csQuestionRepository;

    @PostMapping("/cs-questions")
    @Transactional
    public ApiResponse<SeedCsQuestionsResult> seedCsQuestions(
            @RequestParam(name = "perCategory", defaultValue = "10") int perCategory) {

        Difficulty[] difficulties = Difficulty.values();
        List<CsQuestionJpaEntity> toCreate = new ArrayList<>();

        for (QuestionCategory category : QuestionCategory.values()) {
            for (int i = 1; i <= perCategory; i++) {
                toCreate.add(CsQuestionJpaEntity.builder()
                        .category(category)
                        .question(DEV_SEED_PREFIX + " [" + category.name() + "][" + i + "] "
                                + category.name() + " 관련 질문 " + i + "번입니다.")
                        .difficulty(difficulties[(i - 1) % difficulties.length])
                        .build());
            }
        }

        csQuestionRepository.saveAll(toCreate);
        return ApiResponse.onSuccess(
                new SeedCsQuestionsResult(QuestionCategory.values().length, toCreate.size()));
    }

    public record SeedCsQuestionsResult(int categoryCount, int questionsCreated) {}
}
