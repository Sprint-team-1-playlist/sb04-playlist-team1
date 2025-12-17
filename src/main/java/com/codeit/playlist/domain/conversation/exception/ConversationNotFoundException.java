package com.codeit.playlist.domain.conversation.exception;

import java.util.UUID;

public class ConversationNotFoundException extends ConversationException{

  public ConversationNotFoundException() {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND);
  }

  public static ConversationNotFoundException withConversationId(UUID conversationId) {
    ConversationNotFoundException exception = new ConversationNotFoundException();
    exception.addDetail("conversationId", conversationId);
    return exception;
  }
  public static ConversationNotFoundException withUserId(UUID userId) {
    ConversationNotFoundException exception = new ConversationNotFoundException();
    exception.addDetail("userId", userId);
    return exception;
  }
}
