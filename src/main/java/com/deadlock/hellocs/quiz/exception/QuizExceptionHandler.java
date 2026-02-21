package com.deadlock.hellocs.quiz.exception;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.exception.CustomException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Stream;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.deadlock.hellocs.quiz")
public class QuizExceptionHandler {

    // Quiz 모듈 내부에서 발생한 비즈니스 예외(CustomException) 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
        log.warn("퀴즈 모듈 예외 발생: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getReasonHttpStatus().getHttpStatus())
                .body(ApiResponse.onFailure(e.getErrorCode(), null));
    }

    // 서비스 계층(@Validated, @Valid) 메서드 검증 실패 예외 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        QuizErrorStatus errorStatus = resolveInvalidRequestStatus(e);
        String message = extractMessages(
                e.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        );
        if (message.isBlank()) {
            message = errorStatus.getMessage();
        }

        log.warn("퀴즈/채점 모듈 검증 실패: {}", e.getMessage());
        return toBadRequestResponse(errorStatus, message);
    }

    // 웹 요청 바인딩 및 필드 검증 실패 예외 처리
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        QuizErrorStatus errorStatus = resolveInvalidRequestStatus(e);
        String message = extractMessages(
                e.getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
        );
        if (message.isBlank()) {
            message = errorStatus.getMessage();
        }

        log.warn("퀴즈/채점 요청 바인딩 실패: {}", e.getMessage());
        return toBadRequestResponse(errorStatus, message);
    }

    private ResponseEntity<ApiResponse<Object>> toBadRequestResponse(QuizErrorStatus errorStatus, String message) {
        return ResponseEntity
                .status(errorStatus.getReasonHttpStatus().getHttpStatus())
                .body(ApiResponse.onFailure(errorStatus.getCode(), message, null));
    }

    private QuizErrorStatus resolveInvalidRequestStatus(Throwable throwable) {
        boolean isGradingContext = Stream.of(throwable.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(className -> className.startsWith("com.deadlock.hellocs.quiz.grading"));

        if (isGradingContext) {
            return QuizErrorStatus.GRADING_REQUEST_INVALID;
        }
        return QuizErrorStatus.QUIZ_REQUEST_INVALID;
    }

    private String extractMessages(Stream<String> messages) {
        String message = messages
                .filter(msg -> msg != null && !msg.isBlank())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return message.trim();
    }
}
