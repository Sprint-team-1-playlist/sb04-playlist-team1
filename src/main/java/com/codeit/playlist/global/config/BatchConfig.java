package com.codeit.playlist.global.config;

import com.codeit.playlist.domain.content.batch.MovieTasklet;
import com.codeit.playlist.domain.content.batch.SportContentTasklet;
import com.codeit.playlist.domain.content.batch.TvSeriesTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class BatchConfig {
    @Bean
    public Job contentJob(JobRepository jobRepository, Step movieStep, Step sportStep, Step tvSeriesStep) {
        log.info("[콘텐츠 데이터 관리] contentJob 시작, contentStep : {}", movieStep);
        return new JobBuilder("contentJob",jobRepository)
                .start(movieStep)
                .next(sportStep)
                .next(tvSeriesStep)
                .build();
    }

    @Bean
    public Step movieStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, MovieTasklet movieTasklet) {
        return new StepBuilder("movieStep", jobRepository)
                .tasklet(movieTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step sportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SportContentTasklet sportContentTasklet) {
        return new StepBuilder("sportStep", jobRepository)
                .tasklet(sportContentTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step tvSeriesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, TvSeriesTasklet tvSeriesTasklet) {
        return new StepBuilder("tvSeriesStep", jobRepository)
                .tasklet(tvSeriesTasklet, transactionManager)
                .build();
    }
}
