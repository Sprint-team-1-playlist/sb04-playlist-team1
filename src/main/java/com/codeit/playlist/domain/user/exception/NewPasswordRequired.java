package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class NewPasswordRequired extends UserException{

  public NewPasswordRequired() {
    super(UserErrorCode.NEW_PASSWORD_REQUIRED);
  }

  public static NewPasswordRequired withId(UUID userId) {
    NewPasswordRequired exception = new NewPasswordRequired();
    exception.addDetail("userId", userId);
    return exception;
  }
}
