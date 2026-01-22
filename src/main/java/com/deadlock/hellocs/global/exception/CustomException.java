package com.deadlock.hellocs.global.exception;

import com.deadlock.hellocs.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

/**
 * 서비스/도메인 레이어에서 표준화된 에러코드를 던지기 위한 예외
 */
@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getReason().getMessage());
        this.errorCode = errorCode;
    }
}