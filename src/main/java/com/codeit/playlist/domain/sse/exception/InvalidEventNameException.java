package com.codeit.playlist.domain.sse.exception;

public class InvalidEventNameException extends SseException{
  public InvalidEventNameException() {
    super(SseErrorCode.INVALID_EVENT_NAME);
  }

  public static InvalidEventNameException withEventName(String eventName) {
    InvalidEventNameException exception = new InvalidEventNameException();
    exception.addDetail("eventName", eventName);
    return exception;
  }
}
