package com.codeit.playlist.domain.sse.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SseErrorCode implements ErrorCode {
  INVALID_SSE_EMITTER_EXCEPTION(HttpStatus.BAD_REQUEST.value(), "receiverId 또는 SseEmitter가 null입니다."),
  SSE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SSE 이벤트 전송에 실패했습니다."),
  SSE_PING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SSE ping 전송에 실패했습니다."),
  SSE_RECONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SSE 재연결 시 이벤트 재전송 실패했습니다."),
  INVALID_EVENT_NAME(HttpStatus.BAD_REQUEST.value(), "eventName이 null입니다.");

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
