package com.codeit.playlist.domain.watching.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchingErrorCode implements ErrorCode {
    Watching_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "시청 세션 정보가 없습니다.");

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
