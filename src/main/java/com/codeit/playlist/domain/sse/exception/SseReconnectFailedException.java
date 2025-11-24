package com.codeit.playlist.domain.sse.exception;

import java.util.UUID;

public class SseReconnectFailedException extends SseException {

  public SseReconnectFailedException() {
    super(SseErrorCode.SSE_RECONNECT_FAILED);
  }

  public static SseReconnectFailedException withId(UUID receiverId, UUID eventId) {
    SseReconnectFailedException exception = new SseReconnectFailedException();
    exception.addDetail("receiverId", receiverId);
    exception.addDetail("eventId", eventId);
    return exception;
  }
}
