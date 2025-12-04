package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class OAuth2NotEmailException extends UserException {

  public OAuth2NotEmailException() {
    super(UserErrorCode.OAUTH2_NOT_EMAIL_EXCEPTION);
  }

  public static AuthenticationOAuthJwtException withId(UUID userId) {
    AuthenticationOAuthJwtException exception = new AuthenticationOAuthJwtException();
    exception.addDetail("userId", userId);
    return exception;
  }
}
