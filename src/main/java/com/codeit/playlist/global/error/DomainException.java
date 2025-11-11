package com.codeit.playlist.global.error;

import java.util.Map;

public class DomainException extends BusinessException {
    public DomainException(Map<String, Object> details) {
        super(BaseErrorCode.INVALID_INPUT_VALUE, details);
    }
}
