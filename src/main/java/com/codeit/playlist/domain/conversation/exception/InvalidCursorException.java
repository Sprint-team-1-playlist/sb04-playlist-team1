package com.codeit.playlist.domain.conversation.exception;

public class InvalidCursorException extends ConversationException{

  public InvalidCursorException() {
    super(ConversationErrorCode.INVALID_CURSOR);
  }

  public static InvalidCursorException withCursor(String cursor) {
    InvalidCursorException exception = new InvalidCursorException();
    exception.addDetail("cursor", cursor);
    return exception;
  }
}
