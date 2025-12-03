package com.codeit.playlist.domain.watching.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchingErrorCode implements ErrorCode {
    WATCHING_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "시청 세션 정보가 없습니다."),
    WATCHING_SESSION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "실시간 세션 정보 업데이트 중 오류가 발생했습니다."),
    WATCHING_SESSION_MISMATCH(HttpStatus.BAD_REQUEST.value(), "시청 세션 정보가 일치하지 않습니다."),
    EVENT_BROADCAST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "이벤트 발송에 실패했습니다.");

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
