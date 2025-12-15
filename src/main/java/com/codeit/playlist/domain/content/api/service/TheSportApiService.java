package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TheSportListResponse;
import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheSportApiService {
    private final WebClient webClient;

    @Value("${SPORTSDB_API_KEY}")
    private String apiKey;

    private Mono<TheSportListResponse> callTheSportApi(int leagueId, String season) {
        log.info("[콘텐츠 데이터 관리] TheSport API Mono 빌드 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.thesportsdb.com")
                        .path("/api/v1/json/{apiKey}/eventsseason.php")
                        .queryParam("id", leagueId)
                        .queryParam("s", season)
                        .build(apiKey))
                .retrieve()
                .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(TheSportListResponse.class)
                .retryWhen(
                        Retry.backoff(5, Duration.ofSeconds(5))
                                .maxBackoff(Duration.ofMinutes(1))
                                .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                .transientErrors(true)
                )
                .doOnNext(response -> log.debug("season : {}, totalEvents : {}", season, response.events() == null ? 0 : response.events().size()))
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] TheSports API Mono 빌드 에러 발생", e));
    }

    private Flux<TheSportResponse> fluxingTheSportApi(YearMonth yearMonth) {
        log.info("[콘텐츠 데이터 관리] TheSport API Flux 빌드 시작");
        int leagueId = 4328;
        String season = circulateSoccerSeason(yearMonth);
        return callTheSportApi(leagueId, season)
                .flatMapMany(theSportResponse -> {
                    if(theSportResponse.events() == null || theSportResponse.events().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(theSportResponse.events());
                        })
                .filter(response -> response.dateEvent() != null && !response.dateEvent().isBlank())
                .filter(response -> {
                    try {
                        LocalDate date = LocalDate.parse(response.dateEvent());
                        return YearMonth.from(date).equals(yearMonth);
                    } catch(DateTimeParseException e) {
                        log.warn("[콘텐츠 데이터 관리] TheSport DateEvent 파싱 실패, idEvent={}, dateEvent={}", response.idEvent(), response.dateEvent());
                        return false;
                    }
                })
                .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
                    log.warn("[콘텐츠 데이터 관리] 429 Too many Requests");
                    return Flux.empty();
                });
    }

    private String circulateSoccerSeason(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        return yearMonth.getMonthValue() >= 8 ? (year + "-" + (year + 1)) : ((year - 1) + "-" + year);
    }

    public Flux<TheSportResponse> getApiSport(int year, int month) {
        return fluxingTheSportApi(YearMonth.of(year, month));
    }
}
