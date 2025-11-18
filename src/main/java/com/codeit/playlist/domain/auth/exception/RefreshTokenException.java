package com.codeit.playlist.domain.auth.exception;

public class RefreshTokenException extends AuthException {

  public RefreshTokenException() {
    super(AuthErrorCode.REFRESH_TOKEN_EXCEPTION);
  }

  public static RefreshTokenException withToken(String token) {
    RefreshTokenException exception = new RefreshTokenException();
    String masked = token != null && token.length() > 8
        ? token.substring(0, 8) + "..."
        : "***";
    exception.addDetail("tokenPrefix", masked);
    return exception;
  }
}
