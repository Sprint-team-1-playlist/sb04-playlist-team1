package com.codeit.playlist.global.error;

public class InvalidSortDirectionException extends BusinessException {

  public InvalidSortDirectionException() {
    super(BaseErrorCode.INVALID_SORT_DIRECTION);
  }

  public static InvalidSortDirectionException withSortDirection(String sortDirection) {
    InvalidSortDirectionException exception = new InvalidSortDirectionException();
    exception.addDetail("sortDirection", sortDirection);
    return exception;
  }
}
