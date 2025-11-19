package com.codeit.playlist.domain.auth.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class AuthException extends BusinessException {

  public AuthException(ErrorCode errorCode) {
    super(errorCode);
  }
}
