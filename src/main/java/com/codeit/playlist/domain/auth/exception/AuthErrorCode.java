package com.codeit.playlist.domain.auth.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  ACCESS_DENIED_EXCEPTION(HttpStatus.FORBIDDEN.value(), "권한이 없습니다."),
  REFRESH_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED.value(), "REFRESH 토큰이 필요합니다."),
  INVALID_OR_EXPIRED_EXCEPTION(HttpStatus.BAD_REQUEST.value(), "REFRESH 토큰이 유효하지 않거나 만료되었습니다."),
  JWT_INTERNAL_SERVER_ERROR_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR.value(), "JWT 처리 중 오류 발생"),;

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
