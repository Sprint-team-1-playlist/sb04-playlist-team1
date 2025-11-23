package com.codeit.playlist.domain.message.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MessageErrorCode implements ErrorCode {
  MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "메시지를 찾을 수 없습니다."),
  INVALID_MESSAGE_READ_OPERATION(HttpStatus.BAD_REQUEST.value(), "잘못된 메시지 읽음 처리입니다.");

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
