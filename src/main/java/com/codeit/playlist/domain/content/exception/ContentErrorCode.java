package com.codeit.playlist.domain.content.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    CONTENT_BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "컨텐츠에 대한 잘못된 요청입니다."), // 400
    CONTENT_UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "컨텐츠에 대해 인증되지 않았습니다."), // 401
    CONTENT_FORBIDDEN(HttpStatus.FORBIDDEN.value(), "컨텐츠에 대한 잘못된 접근입니다."), // 403
    CONTENT_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "컨텐츠 서버가 폭발했습니다."); // 500

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
