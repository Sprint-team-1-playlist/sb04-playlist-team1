package com.codeit.playlist.follow.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.exception.FollowAlreadyExistsException;
import com.codeit.playlist.domain.follow.exception.FollowNotFoundException;
import com.codeit.playlist.domain.follow.exception.FollowSelfNotAllowedException;
import com.codeit.playlist.domain.follow.mapper.FollowMapper;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.follow.service.basic.BasicFollowService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class BasicFollowServiceTest {

  @Mock
  private FollowRepository followRepository;

  @Mock
  private FollowMapper followMapper;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private BasicFollowService followService;

  private UUID followeeId;
  private UUID followerId;
  private FollowRequest followRequest;
  private User follower;
  private User followee;
  private Follow follow;

  @BeforeEach
  void setUp() {
    followerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    followeeId = UUID.randomUUID();

    follower = new User("follower@test.com", "pw", "follower", null, Role.USER);
    followee = new User("followee@test.com", "pw", "followee", null, Role.USER);

    setId(follower, followerId);
    setId(followee, followeeId);

    followRequest = new FollowRequest(followeeId);

    follow = new Follow(follower, followee);
    setId(follow, UUID.randomUUID());

    PlaylistUserDetails userDetails = new PlaylistUserDetails(
        new UserDto(follower.getId(), LocalDateTime.now(), follower.getEmail(), follower.getName(),
            follower.getProfileImageUrl(), follower.getRole(), follower.isLocked()),
        follower.getPassword()
    );

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    org.mockito.Mockito.lenient().when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
    org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }


  @Test
  @DisplayName("팔로우 생성 성공")
  void createFollowSuccess() {
    // given
    UUID followeeId = UUID.randomUUID();
    User followee = new User("followee@test.com", "pw", "followee", null, Role.USER);
    setId(followee, followeeId);

    FollowRequest followRequest = new FollowRequest(followeeId);
    Follow follow = new Follow(follower, followee);
    setId(follow, UUID.randomUUID());

    // followerId stub 추가 (SecurityContext에서 가져오는 ID)
    when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));
    when(followRepository.save(any(Follow.class))).thenReturn(follow);
    when(followMapper.toDto(any(Follow.class))).thenReturn(new FollowDto(follow.getId(), followerId, followeeId));

    // when
    FollowDto result = followService.create(followRequest);

    // then
    assertNotNull(result);
    verify(followRepository, times(1)).save(any(Follow.class));
    verify(followMapper, times(1)).toDto(any(Follow.class));
  }

  @Test
  @DisplayName("팔로우 대상 유저가 존재하지 않을 때 400 예외 발생")
  void createFollowFolloweeNotFound() {
    UUID followeeId = UUID.randomUUID();
    FollowRequest followRequest = new FollowRequest(followeeId);

    when(userRepository.findById(followeeId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> followService.create(followRequest));
    verify(userRepository, times(1)).findById(followeeId);
  }

  @Test
  @DisplayName("자기 자신을 팔로우할 경우 400 예외 발생")
  void createFollowSelfFollowNotAllowed() {
    FollowRequest selfRequest = new FollowRequest(followerId);
    assertThrows(FollowSelfNotAllowedException.class, () -> followService.create(selfRequest));
  }

  @Test
  @DisplayName("이미 팔로우 중일 경우 400 예외 발생")
  void createFollowAlreadyFollowing() {
    UUID followeeId = UUID.randomUUID();
    User followee = new User("followee@test.com", "pw", "followee", null, Role.USER);
    setId(followee, followeeId);

    FollowRequest followRequest = new FollowRequest(followeeId);

    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

    assertThrows(FollowAlreadyExistsException.class, () -> followService.create(followRequest));
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중이면 true 반환")
  void followedByMeFollowing() {
    UUID followeeId = UUID.randomUUID();
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

    Boolean result = followService.followedByMe(followeeId);

    assertNotNull(result);
    assertTrue(result);
    verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중이 아니면 false 반환")
  void followedByMeNotFollowing() {
    UUID followeeId = UUID.randomUUID();
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(false);

    Boolean result = followService.followedByMe(followeeId);

    assertNotNull(result);
    assertFalse(result);
    verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수 조회 성공")
  void countFollowersSuccess() {
    UUID followeeId = UUID.randomUUID();
    User followee = new User("followee@test.com", "1234", "followee", null, Role.USER);
    followee.increaseFollowCount();

    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));

    Long result = followService.countFollowers(followeeId);

    assertNotNull(result);
    assertEquals(1L, result);
    verify(userRepository, times(1)).findById(followeeId);
  }

  @Test
  @DisplayName("팔로워 수 조회 시 유저가 존재하지 않으면 예외 발생")
  void countFollowersUserNotFound() {
    UUID invalidId = UUID.randomUUID();
    when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> followService.countFollowers(invalidId));
    verify(userRepository, times(1)).findById(invalidId);
  }

  @Test
  @DisplayName("팔로우 삭제 성공 시 followee의 팔로워 수 감소")
  void deleteFollowSuccess() {
    UUID followId = UUID.randomUUID();
    User followee = new User("followee@test.com", "1234", "followee", null, Role.USER);
    User follower = new User("follower@test.com", "1234", "follower", null, Role.USER);
    Follow follow = new Follow(followee, follower);
    follow.getFollowee().increaseFollowCount();

    when(followRepository.findById(followId)).thenReturn(Optional.of(follow));
    doNothing().when(followRepository).deleteById(followId);

    followService.delete(followId);

    assertEquals(0L, follow.getFollowee().getFollowCount());
    verify(followRepository, times(1)).findById(followId);
    verify(followRepository, times(1)).deleteById(followId);
  }

  @Test
  @DisplayName("팔로우 삭제 시 존재하지 않는 followId면 FollowNotFoundException 발생")
  void deleteFollowNotFound() {
    UUID followId = UUID.randomUUID();
    when(followRepository.findById(followId)).thenReturn(Optional.empty());

    assertThrows(FollowNotFoundException.class, () -> followService.delete(followId));
    verify(followRepository, times(1)).findById(followId);
    verify(followRepository, never()).deleteById(any());
  }

  private void setId(Object entity, UUID id) {
    try {
      Field field = BaseEntity.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
