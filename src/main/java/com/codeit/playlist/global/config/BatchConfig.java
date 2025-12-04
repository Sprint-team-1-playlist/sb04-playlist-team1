package com.codeit.playlist.global.config;

import com.codeit.playlist.domain.content.batch.ContentTasklet;
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
    public Job contentJob(JobRepository jobRepository, Step contentStep) {
        log.info("[콘텐츠 데이터 관리] contentJob 시작, contentStep : {}", contentStep);
        return new JobBuilder("contentJob",jobRepository)
                .start(contentStep)
                .build();
    }

    @Bean
    public Step contentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, ContentTasklet contentTasklet) {
        return new StepBuilder("contentStep", jobRepository)
                .tasklet(contentTasklet, transactionManager)
                .build();
    }
}
