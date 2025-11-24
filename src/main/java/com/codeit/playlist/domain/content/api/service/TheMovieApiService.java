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

    private Mono<TheMovieListResponse> callTheMovieApi(String query, String path) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("query", query)
                        .queryParam("api_key", apikey)
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
                        e -> log.error("TheMovie API {} 수집 오류 : status = {}, body = {}", query, e.getStatusCode(), e.getResponseBodyAsString()));
    }

    private Flux<TheMovieResponse> fluxingTheMovieApi(String query, String path) {
            return callTheMovieApi(query, path)
                    .flatMapMany(res -> Flux.fromIterable(res.results()));
    }

    @Transactional
    public Flux<TheMovieResponse> getApiMovie(String query) {
        return fluxingTheMovieApi(query, "/3/discover/movie");
    }
}
