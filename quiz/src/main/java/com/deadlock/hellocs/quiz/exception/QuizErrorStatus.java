package com.deadlock.hellocs.quiz.exception;

import com.deadlock.hellocs.common.apiPayload.code.BaseErrorCode;
import com.deadlock.hellocs.common.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QuizErrorStatus implements BaseErrorCode {
    QUIZ_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "QUIZ4001", "퀴즈 요청이 올바르지 않습니다."),
    QUIZ_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ5001", "요청한 모드에 대한 퀴즈 생성 정책을 찾을 수 없습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ4041", "퀴즈를 찾을 수 없습니다."),

    QUIZ_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "QUIZ4002", "퀴즈 세션이 존재하지 않습니다. 퀴즈를 먼저 조회하세요."),
    QUIZ_SESSION_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "QUIZ5002", "퀴즈 세션 직렬화에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .message(message)
                .code(code)
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
