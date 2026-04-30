package com.deadlock.hellocs.interview.exception;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.deadlock.hellocs.interview")
public class InterviewExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
        log.warn("면접 모듈 예외 발생: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getReasonHttpStatus().getHttpStatus())
                .body(ApiResponse.onFailure(e.getErrorCode(), null));
    }
}
