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

    @Value("${api.tmdb.key}")
    private String apikey; // tmdb API key

    public String searchMovie(String query) {
        log.info("Movie 수집 시작, query = {}", query); // debug 예정, 확인용 info
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/search/movie")
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
                    ).block();
        } catch(WebClientResponseException e) {
            log.error("TheMovie API Movie 수집 오류 : status = {}, body = {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public String searchTv(String query) {
        try {
            log.debug("TV 수집 시작, query = {}", query);
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/search/tv")
                            .queryParam("query",query)
                            .queryParam("api_key", apikey)
                            .build())
                    .retrieve()
                    .onStatus(s -> s.value() == 429, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .filter(exception -> exception instanceof WebClientResponseException webException && webException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                                    .transientErrors(true)
                    ).block();
        } catch(WebClientResponseException e) {
            log.error("TheMovie API Movie 수집 오류 : status = {}, body = {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
