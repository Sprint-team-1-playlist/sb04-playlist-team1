package com.codeit.playlist.domain.file.exception;

public class FileTooLargeException extends FileException {
  public FileTooLargeException() {
    super(FileErrorCode.FILE_TOO_LARGE);
  }

  public static FileTooLargeException withSize(long size) {
    FileTooLargeException exception = new FileTooLargeException();
    exception.addDetail("size", size);
    return exception;
  }
}
