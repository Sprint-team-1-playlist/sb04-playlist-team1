package com.codeit.playlist.domain.content.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheMovieApiService {
    private final WebClient webClient;

    @Value("${api.tmdb.key}")
    private String apikey; // tmdb API key

    public Mono<String> searchMovie(String query) {
        log.debug("Movie 수집 시작, query = {}", query);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path("/3/search/movie")
                        .queryParam("query", query)
                        .queryParam("api_key", apikey)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> searchTv(String query) {
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
                .bodyToMono(String.class);
    }
}
