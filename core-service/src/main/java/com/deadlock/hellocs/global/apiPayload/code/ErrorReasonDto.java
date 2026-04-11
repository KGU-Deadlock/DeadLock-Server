package com.deadlock.hellocs.global.apiPayload.code;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorReasonDto {
  private HttpStatus httpStatus;
  private final Boolean isSuccess;
  private final String message;
  private final String code;
}
