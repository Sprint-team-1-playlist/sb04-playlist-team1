package com.codeit.playlist.domain.user.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "해당 프로필을 수정할 권한이 없습니다."),

  USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "사용자 정보가 없습니다."),
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), "중복된 이메일로 가입할 수 없습니다."),
  LOGIN_FAILED(HttpStatus.UNAUTHORIZED.value(), "이메일 또는 비밀번호가 올바르지 않습니다."),
  FORBIDDEN_USER_UPDATE(HttpStatus.FORBIDDEN.value(), "해당 사용자에 대한 수정 권한이 없습니다."),
  FORBIDDEN_USER_DELETE(HttpStatus.FORBIDDEN.value(), "해당 사용자에 대한 삭제 권한이 없습니다."),
  REQUEST_INVALID_UUID_FORMAT(HttpStatus.BAD_REQUEST.value(), "유효하지 않은 UUID 형식입니다."),
  NEW_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST.value(), "새 비밀번호는 필수입니다."),
  PASSWORD_MUST_8_CHARACTERS(HttpStatus.BAD_REQUEST.value(), "비밀번호는 최소 8자리 이상이여야합니다."),
  LOCK_STATE_UNCHANGED(HttpStatus.CONFLICT.value(), "잠금 상태가 이전과 같습니다."),

  USER_NAME_REQUIRED(HttpStatus.BAD_REQUEST.value(), "사용자 이름은 필수 입력값입니다.");
  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
