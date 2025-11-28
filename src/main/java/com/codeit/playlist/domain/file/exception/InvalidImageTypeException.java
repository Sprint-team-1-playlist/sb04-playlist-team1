package com.codeit.playlist.domain.file.exception;

public class InvalidImageTypeException extends FileException {
  public InvalidImageTypeException() {
    super(FileErrorCode.INVALID_IMAGE_TYPE);
  }

  public static InvalidImageTypeException withType(String contentType) {
    InvalidImageTypeException exception = new InvalidImageTypeException();
    exception.addDetail("contentType", contentType);
    return exception;
  }
}
