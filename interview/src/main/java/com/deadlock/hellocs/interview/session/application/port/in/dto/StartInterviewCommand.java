package com.deadlock.hellocs.interview.session.application.port.in.dto;

public record StartInterviewCommand(Long userId, String companyName, String position) {}
