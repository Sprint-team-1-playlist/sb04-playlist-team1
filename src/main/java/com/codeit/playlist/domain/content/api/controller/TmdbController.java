package com.codeit.playlist.domain.content.api.controller;

import com.codeit.playlist.domain.content.batch.ContentScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TmdbController {
    private final ContentScheduler scheduler;

    @GetMapping("/adminMovie")
    public void getMovie() {
        scheduler.runContentJob();
    }
}
