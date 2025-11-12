package com.codeit.playlist.domain.conversation.exception.conversation;

import java.util.UUID;

public class ConversationNotFoundException extends ConversationException{

  public ConversationNotFoundException() {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND);
  }

  public static ConversationNotFoundException withId(UUID conversationId) {
    ConversationNotFoundException exception = new ConversationNotFoundException();
    exception.addDetail("conversationId", conversationId);
    return exception;
  }
}
