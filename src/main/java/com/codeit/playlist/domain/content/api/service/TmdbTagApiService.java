package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TheMovieTagListResponse;
import com.codeit.playlist.domain.content.api.response.TheMovieTagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbTagApiService {
    private final WebClient webClient;

    @Value("{TMDB_API_KEY}")
    private String apiKey;

    private Mono<TheMovieTagListResponse> callTheMovieTagApi(String query, String path) {
        log.info("[콘텐츠 데이터 관리] TheMovie API Tag 수집 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("api_key",apiKey)
                        .query(query)
                        .build())
                .retrieve()
                .bodyToMono(TheMovieTagListResponse.class)
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] The Movie API Tag List 수집 오류, status : {}, body : {}", e.getStatusCode(), e.getResponseBodyAsString()));
    }

    public Mono<Map<Integer, String>> getApiMovieTag(String query) {
        return callTheMovieTagApi(query, "/3/genre/movie/list")
                .map(response -> response.genres().stream()
                        .collect(Collectors.toMap(TheMovieTagResponse::id, TheMovieTagResponse::name))
                );
    }
}
