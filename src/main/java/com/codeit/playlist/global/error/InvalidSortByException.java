package com.codeit.playlist.global.error;

public class InvalidSortByException extends BusinessException {

  public InvalidSortByException() {
    super(BaseErrorCode.INVALID_SORT_BY);
  }

  public static InvalidSortByException withSortBy(String sortBy) {
    InvalidSortByException exception = new InvalidSortByException();
    exception.addDetail("sortBy", sortBy);
    return exception;
  }
}
