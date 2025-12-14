package com.codeit.playlist.domain.content.batch;

import com.codeit.playlist.domain.content.api.handler.TheSportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportContentTasklet implements Tasklet {
    private final TheSportHandler theSportHandler;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[콘텐츠 데이터 관리] The Sport 배치 수집 시작");
        YearMonth lastYearMonth = YearMonth.now().minusMonths(1);
        theSportHandler.save(lastYearMonth.getYear(), lastYearMonth.getMonthValue());
        return RepeatStatus.FINISHED;
    }
}
