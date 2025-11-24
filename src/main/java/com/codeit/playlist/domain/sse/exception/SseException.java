package com.codeit.playlist.domain.sse.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class SseException extends BusinessException {

  public SseException(ErrorCode errorCode) {
    super(errorCode);
  }
}
