package com.deadlock.hellocs.quiz.exception;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.exception.CustomException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Stream;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.deadlock.hellocs.quiz")
public class QuizExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
        log.warn("퀴즈 모듈 예외 발생: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getReasonHttpStatus().getHttpStatus())
                .body(ApiResponse.onFailure(e.getErrorCode(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = extractMessages(
                e.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        );
        if (message.isBlank()) {
            message = QuizErrorStatus.QUIZ_REQUEST_INVALID.getMessage();
        }
        log.warn("퀴즈 모듈 검증 실패: {}", e.getMessage());
        return toBadRequestResponse(message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        String message = extractMessages(
                e.getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
        );
        if (message.isBlank()) {
            message = QuizErrorStatus.QUIZ_REQUEST_INVALID.getMessage();
        }
        log.warn("퀴즈 요청 바인딩 실패: {}", e.getMessage());
        return toBadRequestResponse(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = extractMessages(
                e.getAllErrors().stream().map(error -> error.getDefaultMessage())
        );
        if (message.isBlank()) {
            message = QuizErrorStatus.QUIZ_REQUEST_INVALID.getMessage();
        }
        log.warn("퀴즈 요청 검증 실패: {}", e.getMessage());
        return toBadRequestResponse(message);
    }

    private ResponseEntity<ApiResponse<Object>> toBadRequestResponse(String message) {
        return ResponseEntity
                .status(QuizErrorStatus.QUIZ_REQUEST_INVALID.getReasonHttpStatus().getHttpStatus())
                .body(ApiResponse.onFailure(QuizErrorStatus.QUIZ_REQUEST_INVALID.getCode(), message, null));
    }

    private String extractMessages(Stream<String> messages) {
        return messages
                .filter(msg -> msg != null && !msg.isBlank())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("")
                .trim();
    }
}
