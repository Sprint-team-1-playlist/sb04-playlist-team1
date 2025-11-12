package com.codeit.playlist.domain.follow.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class FollowException extends BusinessException {
  public FollowException(ErrorCode errorCode) {
    super(errorCode);
  }
}
