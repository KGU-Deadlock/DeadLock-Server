package com.deadlock.hellocs.grading.application.port.out.dto;

import java.util.List;

public record AiFeedback(
        int score,
        String message,
        List<String> missingKeywords,
        String improvedAnswer
) {
}
