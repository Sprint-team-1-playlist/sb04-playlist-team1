package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class OAuth2NotEmailException extends UserException {

  public OAuth2NotEmailException() {
    super(UserErrorCode.OAUTH2_NOT_EMAIL_EXCEPTION);
  }

  public static OAuth2NotEmailException withId(UUID userId) {
    OAuth2NotEmailException exception = new OAuth2NotEmailException();
    exception.addDetail("userId", userId);
    return exception;
  }
}
