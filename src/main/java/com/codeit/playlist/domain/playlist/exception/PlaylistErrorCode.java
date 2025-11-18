package com.codeit.playlist.domain.playlist.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "플레이리스트를 찾을 수 없습니다."),
    PLAYLIST_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "플레이리스트 접근 권한이 없습니다."),

    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT.value(), "이미 구독한 플레이리스트입니다."),
    NOT_SUBSCRIBED(HttpStatus.BAD_REQUEST.value(), "구독 중이 아닙니다."),
    SELF_SUBSCRIPTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "자신의 플레이리스트는 구독할 수 없습니다.");

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
