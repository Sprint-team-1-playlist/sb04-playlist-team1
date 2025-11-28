package com.codeit.playlist.domain.file.exception;

public class InvalidImageContentException extends FileException {
  public InvalidImageContentException() {
    super(FileErrorCode.FAIL_UPLOAD_TO_S3);
  }

  public static InvalidImageContentException defaultError() {
    InvalidImageContentException exception = new InvalidImageContentException();
    exception.addDetail("이유", "올바르지 않은 이미지");
    return exception;
  }
}
