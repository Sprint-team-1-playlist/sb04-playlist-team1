package com.codeit.playlist.domain.follow.controller;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/follows")
public class FollowController {

  private final FollowService followService;

  @PostMapping
  public ResponseEntity<FollowDto> follow(@RequestBody FollowRequest request) {
    log.info("[Follow] 팔로우 생성 요청: {}", request.toString());
    FollowDto createdFollow = followService.create(request);
    log.debug("[Follow] 팔로우 생성 응답: {}", createdFollow);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(createdFollow);
  }

}
