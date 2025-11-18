package com.codeit.playlist.domain.auth.exception;

public class RefreshTokenException extends AuthException {

  public RefreshTokenException() {
    super(AuthErrorCode.REFRESH_TOKEN_EXCEPTION);
  }

  public static RefreshTokenException withToken(String token) {
    RefreshTokenException exception = new RefreshTokenException();
    exception.addDetail("token", token);
    return exception;
  }
}
