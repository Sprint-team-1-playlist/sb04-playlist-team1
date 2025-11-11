package com.codeit.playlist.global.error;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseErrorCode implements ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST.value(), "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;

    BaseErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
