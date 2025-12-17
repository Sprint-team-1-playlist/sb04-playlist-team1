package com.codeit.playlist.domain.user.exception;

public class AuthenticationOAuthJwtException extends UserException {

  public AuthenticationOAuthJwtException() {
    super(UserErrorCode.AUTHENTICATION_OAUTH2_JWT_EXCEPTION);
  }

  public static AuthenticationOAuthJwtException withException(String message, String e) {
    AuthenticationOAuthJwtException exception = new AuthenticationOAuthJwtException();
    exception.addDetail(message, e);
    return exception;
  }
}
