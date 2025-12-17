package com.codeit.playlist.domain.conversation.exception;

import java.util.UUID;

public class ConversationAlreadyExistsException extends ConversationException {

  public ConversationAlreadyExistsException() {
    super(ConversationErrorCode.CONVERSATION_ALREADY_EXISTS);
  }
  public static ConversationAlreadyExistsException withId(UUID conversationId) {
    ConversationAlreadyExistsException exception = new ConversationAlreadyExistsException();
    exception.addDetail("conversationId", conversationId);
    return exception;
  }
}
