package com.deadlock.hellocs.global.apiPayload.code.status;


import com.deadlock.hellocs.global.apiPayload.code.BaseErrorCode;
import com.deadlock.hellocs.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
  _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
  _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
  _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
  _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
  _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 리소스를 찾을 수 없습니다."),
  _FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "FAMILY404", "유효하지 않은 초대 코드입니다."),
  _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자를 찾을 수 없습니다."),
  _ALREADY_IN_FAMILY(HttpStatus.BAD_REQUEST, "FAMILY400", "이미 속한 가족이 있습니다."),
  _NOT_FOUND_ANSWER(HttpStatus.NOT_FOUND, "QUESTION404","응답을 찾을 수 없습니다." )
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ErrorReasonDto getReason() {
    return ErrorReasonDto.builder()
        .isSuccess(false)
        .message(message)
        .code(code)
        .build();
  }

  @Override
  public ErrorReasonDto getReasonHttpStatus() {
    return ErrorReasonDto.builder()
        .httpStatus(httpStatus)
        .isSuccess(false)
        .code(code)
        .message(message)
        .build();
  }
}
