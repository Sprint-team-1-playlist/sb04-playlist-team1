package com.codeit.playlist.domain.user.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import java.util.UUID;

public class EmailAlreadyExistsException extends UserException {

  public EmailAlreadyExistsException() {
    super(UserErrorCode.EMAIL_ALREADY_EXISTS);
  }

  public static EmailAlreadyExistsException withEmail(String email) {
    EmailAlreadyExistsException exception = new EmailAlreadyExistsException();
    exception.addDetail("email", email);
    return exception;
  }
}
