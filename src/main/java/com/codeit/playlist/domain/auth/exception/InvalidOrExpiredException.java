package com.codeit.playlist.domain.auth.exception;

public class InvalidOrExpiredException extends AuthException {

  public InvalidOrExpiredException() {
    super(AuthErrorCode.INVALID_OR_EXPIRED_EXCEPTION);
  }

  public static InvalidOrExpiredException withToken(String token) {
    InvalidOrExpiredException exception = new InvalidOrExpiredException();
    exception.addDetail("token", token);
    return exception;
  }
}
