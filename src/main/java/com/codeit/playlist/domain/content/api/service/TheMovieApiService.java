package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TheMovieListResponse;
import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheMovieApiService {
    private final WebClient webClient;

    @Value("${TMDB_API_KEY}")
    private String apikey; // tmdb API key

    private final int firstPage = 1;
    private final int maxPage = 5;

    private Mono<TheMovieListResponse> callTheMovieApi(String query, String path, int page) {
        log.info("[콘텐츠 데이터 관리] TheMovie API Mono 빌드 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("api_key", apikey)
                        .query(query + "&page=" + page)
                        .build())
                .retrieve()
                .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(TheMovieListResponse.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                .transientErrors(true)
                )
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] TheMovie API Mono {} 수집 오류 : status = {}, body = {}", query, e.getStatusCode(), e.getResponseBodyAsString()));
    }

    private Flux<TheMovieResponse> fluxingTheMovieApi(String query, String path) {
        log.info("[콘텐츠 데이터 관리] TheMovie API Flux 빌드 시작");
        return callTheMovieApi(query, path, firstPage)
                    .flatMapMany(firstPageResponse -> {
                       if(firstPageResponse.results() == null || firstPageResponse.results().isEmpty()) {
                           return Flux.empty();
                       }

                       int totalPages = firstPageResponse.totalPages();
                        int maxPages = Math.min(totalPages, maxPage);

                       Flux<TheMovieResponse> firstFluxResult = Flux.fromIterable(firstPageResponse.results());
                       if(totalPages <= 1) { // 만약 totalPages가 1보다 작거나 같다면, firstPage를 그대로 내보냄
                           return firstFluxResult;
                       }

                       Flux<TheMovieResponse> afterFluxResult = Flux.range(2, maxPages - 1) // 2부터 maxPage까지
                               .concatMap(page ->
                                       callTheMovieApi(query, path, page)
                                               .flatMapMany(afterResponse -> {
                                                   if(afterResponse.results() == null || afterResponse.results().isEmpty()) {
                                                       return Flux.empty();
                                                   }
                                                   return Flux.fromIterable(afterResponse.results());
                                               })
                               );
                       return Flux.concat(firstFluxResult, afterFluxResult);
                    });
    }

    @Transactional
    public Flux<TheMovieResponse> getApiMovie(String query) {
        return fluxingTheMovieApi(query, "/3/discover/movie");
    }
}
