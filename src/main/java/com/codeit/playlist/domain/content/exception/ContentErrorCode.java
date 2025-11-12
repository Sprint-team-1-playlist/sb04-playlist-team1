package com.codeit.playlist.domain.content.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    CONTENT_BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "Content Bad Request"), // 400
    CONTENT_UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "Content unauthorized"), // 401
    CONTENT_FORBIDDEN(HttpStatus.FORBIDDEN.value(), "Content forbidden"), // 403
    CONTENT_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Content internal server error"); // 500

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
