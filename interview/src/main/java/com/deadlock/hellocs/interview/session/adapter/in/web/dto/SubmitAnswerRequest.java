package com.deadlock.hellocs.interview.session.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
        @NotNull(message = "질문 번호를 입력해주세요.")
        @Min(value = 1, message = "질문 번호는 1 이상이어야 합니다.")
        @Max(value = 5, message = "질문 번호는 5 이하여야 합니다.")
        Integer questionNumber,

        @NotBlank(message = "답변을 입력해주세요.")
        String answerText,

        Integer durationSeconds
) {}
