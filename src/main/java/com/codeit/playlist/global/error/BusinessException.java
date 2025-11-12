package com.codeit.playlist.global.error;

import com.codeit.playlist.global.constant.ErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Instant timestamp;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.details = new HashMap<>();
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    public int getStatus() {
        return errorCode.getStatus();
    }

    public String getErrorCodeName() {
        return errorCode.getErrorCodeName();
    }
}
