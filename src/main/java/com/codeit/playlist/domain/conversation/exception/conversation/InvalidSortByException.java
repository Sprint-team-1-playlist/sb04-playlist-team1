package com.codeit.playlist.domain.conversation.exception.conversation;

public class InvalidSortByException extends ConversationException{

  public InvalidSortByException() {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND);
  }

  public static InvalidSortByException withSortBy(String sortBy) {
    InvalidSortByException exception = new InvalidSortByException();
    exception.addDetail("sortBy", sortBy);
    return exception;
  }
}
