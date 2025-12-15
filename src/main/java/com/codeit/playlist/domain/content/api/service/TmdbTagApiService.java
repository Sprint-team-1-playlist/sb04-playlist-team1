package com.codeit.playlist.domain.content.api.service;

import com.codeit.playlist.domain.content.api.response.TheMovieTagListResponse;
import com.codeit.playlist.domain.content.api.response.TheMovieTagResponse;
import com.codeit.playlist.domain.content.api.response.TvSeriesListResponse;
import com.codeit.playlist.domain.content.api.response.TvSeriesTagListResponse;
import com.codeit.playlist.domain.content.api.response.TvSeriesTagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbTagApiService {
    private final WebClient webClient;

    @Value("${TMDB_API_KEY}")
    private String apiKey;

    private Mono<TheMovieTagListResponse> callTheMovieTagApi(String path) {
        log.info("[콘텐츠 데이터 관리] TheMovie API Tag 수집 시작");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("api_key",apiKey)
                        .queryParam("language","ko-KR")
                        .build())
                .retrieve()
                .bodyToMono(TheMovieTagListResponse.class)
                .timeout(Duration.ofSeconds(10)) // API가 응답하지 않을 때를 대비
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] The Movie API Tag List 수집 오류, status : {}, body : {}", e.getStatusCode(), e.getResponseBodyAsString()));
    }
    
    public Mono<Map<Integer, String>> getApiMovieTag() {
        return callTheMovieTagApi("/3/genre/movie/list")
                .map(response -> {
                    if(response.genres() == null) {
                        return Map.<Integer, String>of(); // NPE를 방지하기 위한 빈 리스트 추가
                    }
                    return response.genres().stream()
                            .collect(Collectors.toMap(
                                    TheMovieTagResponse::genreId,
                                    TheMovieTagResponse::name,
                                    (existing, replacement) -> existing) // 수정
                            );
                });
    }

    private Mono<TvSeriesTagListResponse> callTvSeriesTagApi(String path) {
        log.info("[콘텐츠 데이터 관리] TvSeries API Tag 수집 시작");
        return webClient.get().uri(
                uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path(path)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .bodyToMono(TvSeriesTagListResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(WebClientResponseException.class,
                        e -> log.error("[콘텐츠 데이터 관리] TvSeries API Tag List 수집 오류, status : {}, body : {}", e.getStatusCode(), e.getResponseBodyAsString()));
    }

    public Mono<Map<Integer, String>> getApiTvSeriesTag() {
        return callTvSeriesTagApi("/3/genre/tv/list")
                .map(response -> {
                    if(response.genres() == null) {
                        return Map.of();
                    }
                    return response.genres().stream()
                            .collect(Collectors.toMap(
                                    TvSeriesTagResponse::id,
                                    TvSeriesTagResponse::name,
                                    (existing, replacement) -> existing));
                });
    }
}
