package com.deadlock.hellocs.interview.session.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record StartInterviewRequest(
        @NotBlank(message = "회사명을 입력해주세요.")
        String companyName,

        @NotBlank(message = "지원 직무를 입력해주세요.")
        String position
) {}
