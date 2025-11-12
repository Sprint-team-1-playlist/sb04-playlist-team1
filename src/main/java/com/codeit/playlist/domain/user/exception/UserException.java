package com.codeit.playlist.domain.user.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class UserException extends BusinessException {
  public UserException(ErrorCode errorCode) {
    super(errorCode);
  }
}
