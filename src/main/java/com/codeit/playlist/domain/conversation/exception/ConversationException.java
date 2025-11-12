package com.codeit.playlist.domain.conversation.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class ConversationException extends BusinessException {
  public ConversationException(ErrorCode errorCode) {
    super(errorCode);
  }
}
