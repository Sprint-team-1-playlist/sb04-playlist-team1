package com.codeit.playlist.domain.follow.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements ErrorCode {
  FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "팔로우을 찾을 수 없습니다.");

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
