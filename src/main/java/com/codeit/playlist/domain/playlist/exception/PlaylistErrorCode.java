package com.codeit.playlist.domain.playlist.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
    //플레이리스트
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "플레이리스트를 찾을 수 없습니다."),
    PLAYLIST_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "플레이리스트 접근 권한이 없습니다."),

    //구독
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT.value(), "이미 구독한 플레이리스트입니다."),
    NOT_SUBSCRIBED(HttpStatus.BAD_REQUEST.value(), "구독 중이 아닙니다."),
    SELF_SUBSCRIPTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "자신의 플레이리스트는 구독할 수 없습니다."),
    SUBSCRIBER_COUNT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "플레이리스트 구독자 수 업데이트에 실패했습니다."),

    //플레이리스트 콘텐츠
    PLAYLIST_CONTENT_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), "플레이리스트 안에 이미 해당 콘텐츠가 존재합니다."),
    PLAYLIST_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "플레이리스트 내에 해당 콘텐츠를 찾을 수 없습니다."),;

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
