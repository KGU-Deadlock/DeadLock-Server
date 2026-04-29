package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import java.util.List;

public record GetQuizResult(
        List<OxQuizResult> oxQuizzes,
        List<MultipleChoiceQuizResult> multipleChoiceQuizzes,
        List<ShortAnswerQuizResult> shortAnswerQuizzes,
        List<VoiceQuizResult> voiceQuizzes
) { }
