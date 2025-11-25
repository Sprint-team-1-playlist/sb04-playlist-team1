package com.codeit.playlist.domain.auth.exception;

public class AuthHeaderMissingException extends AuthException {
  public AuthHeaderMissingException() {
    super(AuthErrorCode.AUTH_HEADER_MISSING);
  }
}
