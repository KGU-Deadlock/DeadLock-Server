package com.deadlock.hellocs.grading.exception;

import com.deadlock.hellocs.common.apiPayload.code.BaseErrorCode;
import com.deadlock.hellocs.common.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GradingErrorStatus implements BaseErrorCode {
    GRADING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GRADING4031", "해당 채점 기록에 대한 접근 권한이 없습니다."),
    GRADING_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "GRADING4001", "채점 요청이 올바르지 않습니다."),
    GRADING_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "GRADING4041", "채점 기록을 찾을 수 없습니다."),
    GRADING_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "GRADING4042", "해당 퀴즈의 채점 결과를 찾을 수 없습니다."),
    GRADING_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "GRADING4043", "채점 대상 퀴즈를 찾을 수 없습니다."),
    GRADING_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "GRADING5001", "요청한 퀴즈 유형에 대한 채점 정책을 찾을 수 없습니다."),
    GRADING_AI_EVALUATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GRADING5002", "AI 채점 서비스 호출에 실패했습니다."),
    GRADING_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "GRADING4002", "퀴즈 세션이 존재하지 않습니다. 퀴즈를 먼저 조회하세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder().isSuccess(false).message(message).code(code).build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder().httpStatus(httpStatus).isSuccess(false).code(code).message(message).build();
    }
}
