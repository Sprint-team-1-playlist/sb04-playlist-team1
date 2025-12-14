package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.service.TheSportApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportContentTasklet implements Tasklet {
    private final TheSportApiService theSportApiService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[콘텐츠 데이터 관리] The Sport 배치 수집 시작");
        return null;
    }
}
