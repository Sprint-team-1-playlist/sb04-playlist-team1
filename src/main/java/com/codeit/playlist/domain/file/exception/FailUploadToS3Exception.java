package com.codeit.playlist.domain.file.exception;

public class FailUploadToS3Exception extends FileException {
    public FailUploadToS3Exception() {
        super(FileErrorCode.FAIL_UPLOAD_TO_S3);
    }

    public static FailUploadToS3Exception withBucket(String bucket) {
        FailUploadToS3Exception exception = new FailUploadToS3Exception();
        exception.addDetail("bucket", bucket);
        return exception;
    }
}
