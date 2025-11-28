package com.codeit.playlist.domain.file.exception;

public class FileTooLargeException extends FileException {
  public FileTooLargeException() {
    super(FileErrorCode.FAIL_UPLOAD_TO_S3);
  }

  public static FileTooLargeException withSize(long size) {
    FileTooLargeException exception = new FileTooLargeException();
    exception.addDetail("size", size);
    return exception;
  }
}
