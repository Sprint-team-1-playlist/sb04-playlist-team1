package com.codeit.playlist.domain.file.exception;

public class FailDeleteFromS3 extends FileException {
    public FailDeleteFromS3() {
        super(FileErrorCode.FAIL_DELETE_FROM_S3);
    }

    public static FailDeleteFromS3 withBucket(String bucket) {
        FailDeleteFromS3 exception = new FailDeleteFromS3();
        exception.addDetail("bucket", bucket);
        return exception;
    }
}
