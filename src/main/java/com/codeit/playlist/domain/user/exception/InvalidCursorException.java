package com.codeit.playlist.domain.user.exception;


public class InvalidCursorException extends UserException {

  public InvalidCursorException() {
    super(UserErrorCode.INVALID_CURSOR);
  }

  public static InvalidCursorException withCursor(String cursor) {
    InvalidCursorException exception = new InvalidCursorException();
    exception.addDetail("cursor", cursor);
    return exception;
  }
}
