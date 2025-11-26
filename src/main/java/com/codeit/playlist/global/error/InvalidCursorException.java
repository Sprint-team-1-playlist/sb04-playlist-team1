package com.codeit.playlist.global.error;

public class InvalidCursorException extends BusinessException {

  public InvalidCursorException() {
    super(BaseErrorCode.INVALID_CURSOR);
  }

  public static InvalidCursorException withCursor(String cursor) {
    InvalidCursorException exception = new InvalidCursorException();
    exception.addDetail("cursor", cursor);
    return exception;
  }
}
