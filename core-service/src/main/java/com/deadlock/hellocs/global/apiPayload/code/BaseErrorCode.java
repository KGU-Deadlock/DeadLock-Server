package com.deadlock.hellocs.global.apiPayload.code;

public interface BaseErrorCode {
  ErrorReasonDto getReason();
  ErrorReasonDto getReasonHttpStatus();
}
