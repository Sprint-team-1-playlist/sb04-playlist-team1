package com.codeit.playlist.domain.file.scheduler;

import com.codeit.playlist.domain.file.S3Uploader;
import com.codeit.playlist.global.constant.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogUploadScheduler {
    private final S3Uploader s3Uploader;
    private final S3Properties s3Properties;

    @Scheduled(cron = "0 0 3 * * *") // 3am에 로그 업로드
    public void uploadLog() {
        String date = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
        File file = new File("logs/app." + date + ".log");

        if (!file.exists()) {
            return;
        }

        String key = "logs/" + UUID.randomUUID() + "_" + date + ".log";
        s3Uploader.uploadLogs(s3Properties.getLogsBucket(), key, file);
    }
}
