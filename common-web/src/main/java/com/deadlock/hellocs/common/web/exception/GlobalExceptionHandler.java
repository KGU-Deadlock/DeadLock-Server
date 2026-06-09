package com.deadlock.hellocs.common.web.exception;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({ConstraintViolationException.class, BindException.class, MethodArgumentNotValidException.class, HandlerMethodValidationException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ApiResponse<Object>> handleValidationException(Exception e) {
    log.warn("Validation exception: {}", e.getMessage());
    return ResponseEntity
        .status(ErrorStatus._BAD_REQUEST.getReasonHttpStatus().getHttpStatus())
        .body(ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, null));
  }

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
    log.warn("CustomException: {}", e.getMessage());
    return ResponseEntity
        .status(e.getErrorCode().getReasonHttpStatus().getHttpStatus())
        .body(ApiResponse.onFailure(e.getErrorCode(), null));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
    log.error("RuntimeException: ", e);
    return ResponseEntity
        .status(ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus().getHttpStatus())
        .body(ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
    log.error("Unexpected exception: ", e);
    return ResponseEntity
        .status(ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus().getHttpStatus())
        .body(ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR, null));
  }
}
