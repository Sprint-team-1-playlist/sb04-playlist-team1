package com.codeit.playlist.domain.auth.exception;

import java.util.UUID;

public class AuthAccessDeniedException extends AuthException {

  public AuthAccessDeniedException() {
    super(AuthErrorCode.ACCESS_DENIED_EXCEPTION);
  }

  public static AuthAccessDeniedException withId(UUID id) {
    AuthAccessDeniedException exception = new AuthAccessDeniedException();
    exception.addDetail("id", id);
    return exception;
  }
}
