package com.codeit.playlist.domain.content.api.scheduler;

import com.codeit.playlist.domain.content.api.mapper.TheMovieMapper;
import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TheMovieScheduler {
    private final TheMovieApiService theMovieApiService;
    private final ContentRepository contentRepository;
    private final TheMovieMapper theMovieMapper;

    // 초, 분, 시, 일, 월, 요일
    // 요일 상관없이 매월 1일, 01시에 스케쥴링을 시작함
    // 테스트는 1분마다 실행함
    @Scheduled(cron = "* * * 1 1 *", zone = "Asia/Seoul")
    public void startTheMovieScheduler() {
        log.info("The Movie 스케쥴러 시작, API 데이터 수집");
        String query = "Japan";

        theMovieApiService.getApiMovie(query)
                .map(resp -> theMovieMapper.toContent(resp, Type.MOVIE))
                .doOnNext(content -> contentRepository.save(content))
                .doOnError(e -> log.error(e.getMessage(), e))
                        .subscribe();
        log.info("The Movie 스케쥴러 동작 완료, API 데이터 수집");
    }
}
