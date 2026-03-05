package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.quiz.domain.QuizMultipleChoice;

import java.util.List;

public record MultipleChoiceQuizResult(
        Long id,
        String content,
        List<String> choices
) {
    public static MultipleChoiceQuizResult from(QuizMultipleChoice quiz) {
        return new MultipleChoiceQuizResult(
                quiz.getId(),
                quiz.getContent(),
                safeChoices(quiz.getChoice())
        );
    }

    private static List<String> safeChoices(List<String> choices) {
        return choices == null ? List.of() : List.copyOf(choices);
    }

}
