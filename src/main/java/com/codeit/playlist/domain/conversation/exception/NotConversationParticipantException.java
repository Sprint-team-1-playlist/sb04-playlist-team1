package com.codeit.playlist.domain.conversation.exception;

import java.util.UUID;

public class NotConversationParticipantException extends ConversationException {

  public NotConversationParticipantException() {
    super(ConversationErrorCode.NOT_CONVERSATION_PARTICIPANT);
  }
  public static NotConversationParticipantException withId(UUID currentUserId) {
    NotConversationParticipantException exception = new NotConversationParticipantException();
    exception.addDetail("currentUserId", currentUserId);
    return exception;
  }
}
