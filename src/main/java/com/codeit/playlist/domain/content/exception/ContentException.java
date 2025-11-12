package com.codeit.playlist.domain.content.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class ContentException extends BusinessException {
    public ContentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
