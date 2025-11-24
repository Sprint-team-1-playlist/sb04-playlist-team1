package com.codeit.playlist.domain.sse.exception;

import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class InvalidSseEmitterException extends SseException{
  public InvalidSseEmitterException() {
    super(SseErrorCode.INVALID_SSE_EMITTER_EXCEPTION);
  }

  public static InvalidSseEmitterException withId(UUID receiverId, SseEmitter emitter) {
    InvalidSseEmitterException exception = new InvalidSseEmitterException();
    exception.addDetail("receiverId", receiverId);
    exception.addDetail("SseEmitter", emitter);
    return exception;
  }
}
