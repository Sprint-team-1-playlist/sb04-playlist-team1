package com.codeit.playlist.domain.user.exception;

import com.codeit.playlist.domain.conversation.exception.ConversationException;

public class InvalidCursorException extends ConversationException {

  public InvalidCursorException() {
    super(UserErrorCode.INVALID_CURSOR);
  }

  public static InvalidCursorException withCursor(String cursor) {
    InvalidCursorException exception = new InvalidCursorException();
    exception.addDetail("cursor", cursor);
    return exception;
  }
}
