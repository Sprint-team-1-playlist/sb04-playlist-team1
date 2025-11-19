package com.codeit.playlist.domain.conversation.exception;

import java.util.UUID;

public class SelfChatNotAllowedException extends ConversationException {

  public SelfChatNotAllowedException() {
    super(ConversationErrorCode.SELF_CHAT_NOT_ALLOWED);
  }
  public static SelfChatNotAllowedException withId(UUID id) {
    SelfChatNotAllowedException exception = new SelfChatNotAllowedException();
    exception.addDetail("id", id);
    return exception;
  }
}
