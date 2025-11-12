package com.codeit.playlist.domain.review.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import com.codeit.playlist.global.error.BusinessException;

public class ReviewException extends BusinessException {
    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
