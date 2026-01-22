package com.deadlock.hellocs.global.exception;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
    log.warn("CustomException: {}", e.getMessage());
    return ResponseEntity
        .status(e.getErrorCode().getReasonHttpStatus().getHttpStatus())
        .body(ApiResponse.onFailure(e.getErrorCode(), null));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
    String message = e.getMessage();

    // 메시지에 따라 적절한 에러 코드 결정
    if (message != null) {
      if (message.contains("유효하지 않은 초대 코드") || message.contains("가족을 찾을 수 없습니다")) {
        log.warn("Family not found: {}", message);
        return ResponseEntity
            .status(ErrorStatus._FAMILY_NOT_FOUND.getReasonHttpStatus().getHttpStatus())
            .body(ApiResponse.onFailure(ErrorStatus._FAMILY_NOT_FOUND, null));
      }

      if (message.contains("사용자를 찾을 수 없습니다")) {
        log.warn("User not found: {}", message);
        return ResponseEntity
            .status(ErrorStatus._USER_NOT_FOUND.getReasonHttpStatus().getHttpStatus())
            .body(ApiResponse.onFailure(ErrorStatus._USER_NOT_FOUND, null));
      }

      if (message.contains("이미 속한 가족이 있습니다")) {
        log.warn("Already in family: {}", message);
        return ResponseEntity
            .status(ErrorStatus._ALREADY_IN_FAMILY.getReasonHttpStatus().getHttpStatus())
            .body(ApiResponse.onFailure(ErrorStatus._ALREADY_IN_FAMILY, null));
      }
    }

    // 기본값: 500 Internal Server Error
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
