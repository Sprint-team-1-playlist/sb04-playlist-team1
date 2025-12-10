package com.codeit.playlist.domain.content.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final JobLauncher jobLauncher;
    private final Job contentJob;

    @Scheduled(cron = "0 0 17 * * *") // 매일 17시에 실행
    public void runContentJob() {
        log.info("[콘텐츠 데이터 관리] API 배치 작업 시작");
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time : ",System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(contentJob, jobParameters);
        } catch(Exception e) {
            log.error("[콘텐츠 데이터 관리] API 배치 작업 실패 : ", e);
        }
    }
}
