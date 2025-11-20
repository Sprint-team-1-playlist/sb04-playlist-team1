package com.codeit.playlist.domain.file.exception;

public class FailGeneratePresignedUrl extends FileException {
    public FailGeneratePresignedUrl() {
        super(FileErrorCode.FAIL_GENERATE_PRESIGNED_URL);
    }

    public static FailGeneratePresignedUrl withBucket(String bucket) {
        FailGeneratePresignedUrl exception = new FailGeneratePresignedUrl();
        exception.addDetail("bucket", bucket);
        return exception;
    }
}
