package com.codeit.playlist.domain.sse.exception;

import java.util.UUID;

public class SseSendFailedException extends SseException {

  public SseSendFailedException() {
    super(SseErrorCode.SSE_SEND_FAILED);
  }

  public static SseSendFailedException withId(UUID receiverId, UUID eventId) {
    SseSendFailedException exception = new SseSendFailedException();
    exception.addDetail("receiverId", receiverId);
    exception.addDetail("eventId", eventId);
    return exception;
  }
}
