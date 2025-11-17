package com.codeit.playlist.domain.conversation.exception.conversation;

public class InvalidCursorException extends ConversationException{

  public InvalidCursorException() {
    super(ConversationErrorCode.INVALID_CURSOR_FORMAT);
  }

  public static InvalidCursorException withCursor(String cursor) {
    InvalidCursorException exception = new InvalidCursorException();
    exception.addDetail("cursor", cursor);
    return exception;
  }
}
