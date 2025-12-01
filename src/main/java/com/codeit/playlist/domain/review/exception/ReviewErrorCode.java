package com.codeit.playlist.domain.review.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "리뷰를 찾을 수 없습니다."),
    REVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "리뷰 수정 권한이 없습니다."),;

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
