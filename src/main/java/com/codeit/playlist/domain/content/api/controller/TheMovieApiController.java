package com.codeit.playlist.domain.content.api.controller;

import com.codeit.playlist.domain.content.api.service.TheMovieApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TheMovieApiController {
    private final TheMovieApiService theMovieApiService;

    @GetMapping("/movie")
    public String searchMovie(@RequestParam String query) {
        return theMovieApiService.searchMovie(query);
    }

    @GetMapping("/tv")
    public String searchTv(@RequestParam String query) {
        return theMovieApiService.searchTv(query);
    }
}
