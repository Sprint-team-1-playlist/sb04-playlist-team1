package com.codeit.playlist.domain.message.exception;

import java.util.UUID;

public class InvalidMessageReadOperationException extends MessageException{

  public InvalidMessageReadOperationException() {
    super(MessageErrorCode.INVALID_MESSAGE_READ_OPERATION);
  }
  public static InvalidMessageReadOperationException withId(UUID messageId) {
    InvalidMessageReadOperationException exception = new InvalidMessageReadOperationException();
    exception.addDetail("messageId", messageId);
    return exception;
  }
}
