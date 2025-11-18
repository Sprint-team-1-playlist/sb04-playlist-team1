package com.codeit.playlist.domain.message.exception.message;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class MessageException extends BusinessException {
  public MessageException(ErrorCode errorCode) {
    super(errorCode);
  }
}
