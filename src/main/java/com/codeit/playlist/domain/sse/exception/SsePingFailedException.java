package com.codeit.playlist.domain.sse.exception;

import java.util.UUID;

public class SsePingFailedException extends SseException {
  public SsePingFailedException() {
    super(SseErrorCode.SSE_PING_FAILED);
  }

  public static SsePingFailedException withId(UUID receiverId, UUID eventId) {
    SsePingFailedException exception = new SsePingFailedException();
    exception.addDetail("receiverId", receiverId);
    exception.addDetail("eventId", eventId);
    return exception;
  }
}
