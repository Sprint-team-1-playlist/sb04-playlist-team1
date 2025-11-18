package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class PassWordMustCharacters extends UserException{

  public PassWordMustCharacters() {
    super(UserErrorCode.PASSWORD_MUST_8_CHARACTERS);
  }

  public static PassWordMustCharacters withId(UUID userId) {
    PassWordMustCharacters exception = new PassWordMustCharacters();
    exception.addDetail("userId", userId);
    return exception;
  }
}
