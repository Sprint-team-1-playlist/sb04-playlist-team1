package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TheSportListResponse;
import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheSportApiService {
    private final WebClient webClient;

    private Mono<TheSportListResponse> callTheSportApi(LocalDate localDate) {
        log.info("[콘텐츠 데이터 관리] TheSport API Mono 빌드 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.thesportsdb.com")
                        .path("/api/v1/json/123/eventstv.php")
                        .queryParam("d", localDate.toString())
                        .build())
                .retrieve()
                .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(TheSportListResponse.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(3))
                                .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                .transientErrors(true)
                )
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] TheSports API Mono 빌드 에러 발생", e));
    }

    private Flux<TheSportResponse> fluxingTheSportApi(LocalDate localDate) {
        log.info("[콘텐츠 데이터 관리] TheSport API Flux 빌드 시작");
        return callTheSportApi(localDate)
                .flatMapMany(theSportResponse -> {
                    if(theSportResponse.tvevents() == null || theSportResponse.tvevents().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(theSportResponse.tvevents());
                        }
                );
    }

    public Flux<TheSportResponse> getApiSport(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        int days = yearMonth.lengthOfMonth();

        return Flux.range(0, days)
                .map(startDate::plusDays) // startDate부터 plusDay해줌
                .concatMap(date ->
                        fluxingTheSportApi(date) // 날짜별로 호출함
                                .delaySubscription(Duration.ofMillis(600))
                );
    }
}
