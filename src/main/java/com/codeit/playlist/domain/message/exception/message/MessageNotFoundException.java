package com.codeit.playlist.domain.message.exception.message;

import java.util.UUID;

public class MessageNotFoundException extends MessageException{

  public MessageNotFoundException() {
    super(MessageErrorCode.MESSAGE_NOT_FOUND);
  }
  public static MessageNotFoundException withId(UUID messageId) {
    MessageNotFoundException exception = new MessageNotFoundException();
    exception.addDetail("messageId", messageId);
    return exception;
  }
}
