package com.codeit.playlist.domain.file.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements ErrorCode {
    FAIL_UPLOAD_TO_S3(HttpStatus.INTERNAL_SERVER_ERROR.value(), "S3에 파일 업로드를 실패했습니다."),
    FAIL_DELETE_FROM_S3(HttpStatus.INTERNAL_SERVER_ERROR.value(), "S3에서 파일 삭제를 실패했습니다."),
    FAIL_GENERATE_PRESIGNED_URL(HttpStatus.INTERNAL_SERVER_ERROR.value(), "S3 Presigned URL 생성에 실패했습니다."),

    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST.value(), "지원되지 않는 이미지 타입입니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST.value(), "파일 크기 제한을 초과했습니다."),
    INVALID_IMAGE_CONTENT(HttpStatus.BAD_REQUEST.value(), "유효하지 않은 이미지 파일입니다.");

    private final int status;
    private final String message;

    @Override
    public String getErrorCodeName() {
        return name();
    }
}
