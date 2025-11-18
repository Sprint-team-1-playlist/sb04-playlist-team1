package com.codeit.playlist.domain.content.api.controller;

import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TheMovieApiController {
    private final TheMovieApiService theMovieApiService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> getMovie(@RequestParam String query) {
        return theMovieApiService.getApiMovie(query);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> getTv(@RequestParam String query) {
        return theMovieApiService.getApiTv(query);
    }
}
