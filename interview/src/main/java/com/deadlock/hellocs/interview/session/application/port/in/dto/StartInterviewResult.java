package com.deadlock.hellocs.interview.session.application.port.in.dto;

import java.util.List;

public record StartInterviewResult(String interviewId, List<QuestionResult> questions) {}
