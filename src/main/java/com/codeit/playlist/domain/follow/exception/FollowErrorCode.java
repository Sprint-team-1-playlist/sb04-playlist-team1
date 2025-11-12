package com.codeit.playlist.domain.follow.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements ErrorCode {
  FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "팔로우을 찾을 수 없습니다."),
  FOLLOW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST.value(), "이미 팔로우 중인 사용자입니다."),
  FOLLOW_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "자기 자신을 팔로우 할 수 없습니다.");

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
