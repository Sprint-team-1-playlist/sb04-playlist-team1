package com.codeit.playlist.domain.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PlaylistDeleteScheduler {

    private final JobLauncher jobLauncher;
    private final Job playlistHardDeleteJob;

    @Scheduled(cron = "0 0 8 * * *")
    public void runHardDelete() throws JobExecutionException {
        log.info("[Scheduler] 플레이리스트 하드 삭제 배치 실행");

        jobLauncher.run(
                playlistHardDeleteJob,
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters()
        );
    }
}
