package com.codeit.playlist.domain.content.api.controller;

import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import com.codeit.playlist.domain.content.api.service.TheSportsApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TheSportsApiController {
    private final TheSportsApiService theSportsApiService;

    @GetMapping
    public List<TheSportsResponse> getSports(@RequestParam int year,
                                             @RequestParam int month) {
        return theSportsApiService.searchSports(year, month);
    }
}
