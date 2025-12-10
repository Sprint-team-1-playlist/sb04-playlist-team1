package com.codeit.playlist.domain.content.api.scheduler;

import com.codeit.playlist.domain.content.api.service.TheSportsApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TheSportsScheduler {
    private final TheSportsApiService theSportsApiService;

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void startTheSportsScheduler() {
        log.info("[콘텐츠 데이터 관리] The Sports 스케쥴러 시작, API 데이터 수집");
        LocalDate localDate = LocalDate.now().minusDays(1);
        theSportsApiService.saveContentsUsingContents(localDate);
        log.info("[콘텐츠 데이터 관리] The Sports 스케쥴러 동작 완료, API 데이터 수집, localDate = {}", localDate);
    }
}
