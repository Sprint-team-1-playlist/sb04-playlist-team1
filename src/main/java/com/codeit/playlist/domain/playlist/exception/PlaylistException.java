package com.codeit.playlist.domain.playlist.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class PlaylistException extends BusinessException {
    public PlaylistException(ErrorCode errorCode) {
        super(errorCode);
    }
}
