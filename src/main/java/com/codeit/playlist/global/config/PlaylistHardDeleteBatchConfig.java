package com.codeit.playlist.global.config;

import com.codeit.playlist.domain.playlist.service.basic.PlaylistHardDeleteBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class PlaylistHardDeleteBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager; //트랜잭션 관리자
    private final PlaylistHardDeleteBatchService hardDeleteService;

    @Bean
    public Job playlistHardDeleteJob() {
        return new JobBuilder("playlistHardDeleteJob", jobRepository)
                .start(playlistHardDeleteStep())
                .build();
    }

    @Bean
    public Step playlistHardDeleteStep() {
        return new StepBuilder("playlistHardDeleteStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    hardDeleteService.hardDeletedPlaylists();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
