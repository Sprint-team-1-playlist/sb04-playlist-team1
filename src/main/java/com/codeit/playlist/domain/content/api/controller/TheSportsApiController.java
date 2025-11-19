package com.codeit.playlist.domain.content.api.controller;

import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import com.codeit.playlist.domain.content.api.service.TheSportsApiService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TheSportsApiController {
    private final TheSportsApiService theSportsApiService;

    @GetMapping("/sports")
    public List<TheSportsResponse> getSports(@RequestParam @Min(2000) @Max(2100) int year,
                                             @RequestParam @Min(1) @Max(12) int month) {
        return theSportsApiService.searchSports(year, month);
    }
}
