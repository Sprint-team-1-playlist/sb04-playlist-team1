package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TvSeriesListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TvSeriesApiService {
    private final WebClient webClient;

    @Value("${TMDB_API_KEY}")
    private String apikey;

    private final int firstPage = 1;
    private final int maxPage = 2;

    private Mono<TvSeriesListResponse> callTheTvSeriesApi(String query, String path, int page) {
        log.info("[콘텐츠 데이터 관리] TvSeries API Mono 빌드 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("api_key",apikey)
                        .query(query + "&page=" + page)
                        .build())
                .retrieve()
                .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(TvSeriesListResponse.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .filter(exception -> exception instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                .transientErrors(true)
                )
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] TvSeries API Mono {} 수집 오류 : status = {}, body = {}", query, e.getStatusCode(), e.getResponseBodyAsString())
                );
    }
}
