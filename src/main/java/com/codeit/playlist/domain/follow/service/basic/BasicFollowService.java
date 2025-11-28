package com.codeit.playlist.domain.follow.service.basic;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.exception.FollowAlreadyExistsException;
import com.codeit.playlist.domain.follow.exception.FollowNotFoundException;
import com.codeit.playlist.domain.follow.exception.FollowSelfNotAllowedException;
import com.codeit.playlist.domain.follow.mapper.FollowMapper;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.follow.service.FollowService;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class BasicFollowService implements FollowService {

  private final FollowRepository followRepository;
  private final FollowMapper followMapper;
  private final UserRepository userRepository;

  private final ObjectMapper objectMapper;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public FollowDto create(FollowRequest followRequest) {
    log.debug("[Follow] 팔로우 생성 시작: {}", followRequest);

    UUID followerId = getCurrentUserId();

    if (followerId.equals(followRequest.followeeId())) {
      throw FollowSelfNotAllowedException.withId(followRequest.followeeId());
    }

    User followee = userRepository.findById(followRequest.followeeId())
        .orElseThrow(() -> UserNotFoundException.withId(followRequest.followeeId()));

    boolean alreadyFollowing = followRepository.existsByFollowerIdAndFolloweeId(
        followerId, followRequest.followeeId());
    if (alreadyFollowing) {
      throw FollowAlreadyExistsException.withId(followRequest.followeeId());
    }

    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> UserNotFoundException.withId(followerId));

    Follow follow = new Follow(follower, followee);
    followRepository.save(follow);

    followee.increaseFollowCount();

    //팔로우 당한 사용자에게 알림
    String title = "새로운 팔로워";
    String contentMsg = String.format("%s 님이 사용자님을 팔로우하기 시작했습니다.", follower.getName());
    NotificationDto notificationDto = new NotificationDto(null, null, followee.getId(),
                                                          title, contentMsg, Level.INFO);

    try{
      String payload = objectMapper.writeValueAsString(notificationDto);
      log.info("[Follow] 팔로우 알림 이벤트 발행 완료 : receiverId= {}, followerId= {}",
              followee.getId(), followerId);
    } catch (JsonProcessingException e) {
      log.error("[Follow] 팔로우 알림 이벤트 직렬화 실패 : receiverId= {}, followerId= {}",
              followee.getId(), followerId, e);
    }

    log.info("[Follow] 팔로우 생성 완료: {} -> {}", follower.getId(), followee.getId());
    FollowDto followDto = followMapper.toDto(follow);
    return followDto;
  }

  @Override
  public Boolean followedByMe(UUID followeeId) {
    log.debug("[Follow] 특정 유저를 내가 팔로우하는지 여부 조회 시작: {}", followeeId);

    UUID followerId = getCurrentUserId();
    boolean isFollowing = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);

    log.info("[Follow] 특정 유저를 내가 팔로우하는지 여부 조회 완료: {} -> {}", followeeId, isFollowing);
    return isFollowing;
  }

  @Override
  public Long countFollowers(UUID followeeId) {
    log.debug("[Follow] 특정 유저의 팔로워 수 조회 시작: {}", followeeId);

    User followee = userRepository.findById(followeeId)
        .orElseThrow(() -> UserNotFoundException.withId(followeeId));
    long followersCount = followee.getFollowCount();

    log.info("[Follow] 특정 유저의 팔로워 수 조회 완료: {}", followersCount);
    return followersCount;
  }

  @Override
  public void delete(UUID followId) {
    log.debug("[Follow] 팔로우 삭제 시작: {}", followId);
    Follow follow = followRepository.findById(followId)
        .orElseThrow(() -> FollowNotFoundException.withId(followId));
    follow.getFollowee().decreaseFollowCount();
    followRepository.deleteById(followId);
    log.info("[Follow] 팔로우 삭제 완료");
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    return userDetails.getUserDto().id();
  }
}
