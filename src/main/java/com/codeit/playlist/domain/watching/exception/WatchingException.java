package com.codeit.playlist.domain.watching.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class WatchingException extends BusinessException {
    public WatchingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
