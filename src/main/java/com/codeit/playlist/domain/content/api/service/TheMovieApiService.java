package com.codeit.playlist.domain.content.api.service;

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
public class TheMovieApiService {
    private final WebClient webClient;

    @Value("${TMDB_API_KEY}")
    private String apikey; // tmdb API key

    public Mono<String> getApiMovie(String query) {
        return searchTheMovieApi(query, "Movie", "/3/search/movie");
    }

    public Mono<String> getApiTv(String query) {
        return searchTheMovieApi(query, "Tv", "/3/search/tv");
    }

    private Mono<String> searchTheMovieApi(String query, String type, String path) {
        log.info("TheMovie API {} 수집 시작, query = {}", type, query); // type 매개변수, Movie or TV, debug로 수정 필요 - 확인용 info
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path(path) // path 매개변수, /3/search/movie or /3/search/tv
                            .queryParam("query", query)
                            .queryParam("api_key", apikey)
                            .build())
                    .retrieve()
                    .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                    .transientErrors(true)
                    )
                    .doOnError(WebClientResponseException.class,
                            e -> log.error("TheMovie API {} 수집 오류 : status = {}, body = {}", query, e.getStatusCode(), e.getResponseBodyAsString()));
    }
}
