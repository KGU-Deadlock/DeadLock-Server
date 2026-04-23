package com.deadlock.hellocs.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.deadlock.hellocs.global.apiPayload.code.BaseCode;
import com.deadlock.hellocs.global.apiPayload.code.BaseErrorCode;
import com.deadlock.hellocs.global.apiPayload.code.ErrorReasonDto;
import com.deadlock.hellocs.global.apiPayload.code.status.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({ "isSuccess", "code", "message", "data" })
public class ApiResponse<T> {
  @JsonProperty("isSuccess")
  private final boolean isSuccess;
  private final String code;
  private final String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private T data;

  public static <T> ApiResponse<T> onSuccess(T data) {
    return new ApiResponse<>(
        true,
        SuccessStatus._OK.getCode(),
        SuccessStatus._OK.getMessage(),
        data);
  }

  public static <T> ApiResponse<T> of(BaseCode code, String message, T data) {
    return new ApiResponse<>(
        true,
        code.getReasonHttpStatus().getCode(),
        code.getReasonHttpStatus().getMessage(),
        data);
  }

  public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode, T data) {
    ErrorReasonDto reason = errorCode.getReasonHttpStatus();
    return new ApiResponse<>(
        reason.getIsSuccess(),
        reason.getCode(),
        reason.getMessage(),
        data);
  }

  public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
    return new ApiResponse<>(false, code, message, data);
  }
}
