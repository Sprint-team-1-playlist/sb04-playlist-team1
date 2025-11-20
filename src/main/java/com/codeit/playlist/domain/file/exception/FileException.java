package com.codeit.playlist.domain.file.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class FileException extends BusinessException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
