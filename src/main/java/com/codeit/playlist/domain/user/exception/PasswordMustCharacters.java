package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class PasswordMustCharacters extends UserException{

  public PasswordMustCharacters() {
    super(UserErrorCode.PASSWORD_MUST_8_CHARACTERS);
  }

  public static PasswordMustCharacters withId(UUID userId) {
    PasswordMustCharacters exception = new PasswordMustCharacters();
    exception.addDetail("userId", userId);
    return exception;
  }
}
