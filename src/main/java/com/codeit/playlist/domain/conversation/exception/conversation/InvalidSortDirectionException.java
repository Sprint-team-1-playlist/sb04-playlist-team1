package com.codeit.playlist.domain.conversation.exception.conversation;

public class InvalidSortDirectionException extends ConversationException{

  public InvalidSortDirectionException() {
    super(ConversationErrorCode.INVALID_SORT_DIRECTION);
  }

  public static InvalidSortDirectionException withSortDirection(String sortDirection) {
    InvalidSortDirectionException exception = new InvalidSortDirectionException();
    exception.addDetail("sortDirection", sortDirection);
    return exception;
  }
}
