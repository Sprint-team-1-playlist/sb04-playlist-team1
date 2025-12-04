package com.codeit.playlist.domain.user.exception;

public class AuthenticationOAuthJwtException extends UserException {

  public AuthenticationOAuthJwtException() {
    super(UserErrorCode.AUTHENTICATION_OAUTH2_JWT_EXCEPTION);
  }

  public static AuthenticationOAuthJwtException withException(String e) {
    AuthenticationOAuthJwtException exception = new AuthenticationOAuthJwtException();
    exception.addDetail("exception", e);
    return exception;
  }
}
