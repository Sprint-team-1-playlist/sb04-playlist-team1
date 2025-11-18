package com.codeit.playlist.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SseController {

  @GetMapping("/api/sse")
  public ResponseEntity<String> dummy() {
    return ResponseEntity.ok("SSE temporarily disabled");
  }
}
