package com.codeit.playlist.domain.content.api.scheduler;

import com.codeit.playlist.domain.content.api.mapper.TheMovieMapper;
import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TheMovieScheduler {
    private final TheMovieApiService theMovieApiService;
    private final ContentRepository contentRepository;
    private final TheMovieMapper theMovieMapper;
    private Disposable movieSubscription;

    // 초, 분, 시, 일, 월, 요일
    // 프로덕션은 요일 상관없이 매월 1일, 01시에 스케쥴링을 시작함
    // 테스트는 30초마다 실행함
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void startTheMovieScheduler() {
        // 이전 구독이 있으면 정리하는 조건문
        if(movieSubscription != null && !movieSubscription.isDisposed()) {
          movieSubscription.dispose();
        }
        log.info("The Movie 스케쥴러 시작, API 데이터 수집");
        String query = "Japan";

        movieSubscription = theMovieApiService.getApiMovie(query)
                .map(response -> theMovieMapper.toContent(response, Type.MOVIE))
                .doOnNext(content -> contentRepository.save(content))
                .doOnComplete(() -> log.info("The Movie 스케줄러 동작 완료, API 데이터 수집"))
                .doOnError(e -> log.error("The Movie 스케줄러 에러 발생 : {}", e.getMessage(), e))
                .subscribe();
    }
}
