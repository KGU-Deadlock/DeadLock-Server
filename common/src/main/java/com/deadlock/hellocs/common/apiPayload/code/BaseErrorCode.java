package com.deadlock.hellocs.common.apiPayload.code;

public interface BaseErrorCode {
  ErrorReasonDto getReason();
  ErrorReasonDto getReasonHttpStatus();
}
