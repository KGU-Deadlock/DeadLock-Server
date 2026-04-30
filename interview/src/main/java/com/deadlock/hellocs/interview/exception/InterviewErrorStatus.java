package com.deadlock.hellocs.interview.exception;

import com.deadlock.hellocs.global.apiPayload.code.BaseErrorCode;
import com.deadlock.hellocs.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InterviewErrorStatus implements BaseErrorCode {

    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW4041", "면접 세션을 찾을 수 없습니다."),
    INTERVIEW_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "INTERVIEW4001", "이미 완료된 면접 세션입니다."),
    INTERVIEW_ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW4042", "해당 면접의 답변을 찾을 수 없습니다."),
    INTERVIEW_FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW4043", "면접 피드백을 찾을 수 없습니다."),
    INTERVIEW_CS_QUESTION_NOT_ENOUGH(HttpStatus.INTERNAL_SERVER_ERROR, "INTERVIEW5001", "CS 질문이 부족합니다. 최소 2개 이상의 질문이 필요합니다."),
    INTERVIEW_AI_FEEDBACK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERVIEW5002", "AI 피드백 생성에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }
}
