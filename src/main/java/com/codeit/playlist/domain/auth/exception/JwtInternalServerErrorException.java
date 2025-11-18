package com.codeit.playlist.domain.auth.exception;

public class JwtInternalServerErrorException extends AuthException{

  public JwtInternalServerErrorException() {
    super(AuthErrorCode.JWT_INTERNAL_SERVER_ERROR_EXCEPTION);
  }

  public static JwtInternalServerErrorException jwtError(Throwable error) {
    JwtInternalServerErrorException exception = new JwtInternalServerErrorException();
    exception.addDetail("error", error);
    return exception;

  }
}
