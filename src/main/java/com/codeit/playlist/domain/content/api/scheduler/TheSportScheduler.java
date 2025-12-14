package com.codeit.playlist.domain.content.api.scheduler;

import com.codeit.playlist.domain.content.api.handler.TheSportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TheSportScheduler {
    private final TheSportHandler theSportHandler;

    @Scheduled(cron = "0/30 * * * * *", zone = "Asia/Seoul")
    public void startTheSportScheduler() {
        YearMonth lastYearMonth = YearMonth.now().minusMonths(1);
        theSportHandler.save(lastYearMonth.getYear(), lastYearMonth.getMonthValue());
    }
}
