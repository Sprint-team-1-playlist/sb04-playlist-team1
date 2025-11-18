package com.codeit.playlist.domain.conversation.exception;

public class InvalidSortByException extends ConversationException{

  public InvalidSortByException() {
    super(ConversationErrorCode.INVALID_SORT_BY);
  }

  public static InvalidSortByException withSortBy(String sortBy) {
    InvalidSortByException exception = new InvalidSortByException();
    exception.addDetail("sortBy", sortBy);
    return exception;
  }
}
