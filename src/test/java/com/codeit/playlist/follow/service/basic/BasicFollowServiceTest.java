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

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.exception.FollowAlreadyExistsException;
import com.codeit.playlist.domain.follow.exception.FollowNotFoundException;
import com.codeit.playlist.domain.follow.exception.FollowSelfNotAllowedException;
import com.codeit.playlist.domain.follow.mapper.FollowMapper;
import com.codeit.playlist.domain.follow.repository.FollowRepository;
import com.codeit.playlist.domain.follow.service.basic.BasicFollowService;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BasicFollowServiceTest {

  @Mock
  private FollowRepository followRepository;

  @Mock
  private FollowMapper followMapper;

  @Mock
  private UserRepository userRepository;

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
    followerId = UUID.fromString("11111111-1111-1111-1111-111111111111"); // 테스트용 임시 ID
    followeeId = UUID.randomUUID();
    followRequest = new FollowRequest(followeeId);

    follower = new User("follower@test.com", "1234", "follower", null, Role.USER);
    followee = new User("followee@test.com", "1234", "followee", null, Role.USER);

    follow = new Follow(followee, follower);
  }

  @Test
  @DisplayName("팔로우 생성 성공")
  void createFollowSuccess() {
    // given
    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));
    when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
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
    // given: userRepository에서 followee를 찾을 수 없도록 설정
    when(userRepository.findById(followeeId)).thenReturn(Optional.empty());

    // when then: UserNotFoundException이 발생해야 함
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
    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

    assertThrows(FollowAlreadyExistsException.class, () -> followService.create(followRequest));
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중이면 true 반환")
  void followedByMeFollowing() {
    // given
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

    // when
    Boolean result = followService.followedByMe(followeeId);

    // then
    assertNotNull(result);
    assertTrue(result);
    verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중이 아니면 false 반환")
  void followedByMeNotFollowing() {
    // given
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(false);

    // when
    Boolean result = followService.followedByMe(followeeId);

    // then
    assertNotNull(result);
    assertFalse(result);
    verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  @Test
  @DisplayName("자기 자신을 조회하면 예외 발생")
  void followedByMeSelfFollowNotAllowed() {
    UUID selfId = followerId;

    // then
    assertThrows(FollowSelfNotAllowedException.class, () -> followService.followedByMe(selfId));
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수 조회 성공")
  void countFollowersSuccess() {
    // given
    UUID followeeId = UUID.randomUUID();
    User followee = new User("followee@test.com", "1234", "followee", null, Role.USER);
    followee.increaseFollowCount();

    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));

    // when
    Long result = followService.countFollowers(followeeId);

    // then
    assertNotNull(result);
    assertTrue(result >= 0);
    assertTrue(result == 1L);
    verify(userRepository, times(1)).findById(followeeId);
  }

  @Test
  @DisplayName("팔로워 수 조회 시 유저가 존재하지 않으면 예외 발생")
  void countFollowersUserNotFound() {
    // given
    UUID invalidId = UUID.randomUUID();
    when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(UserNotFoundException.class, () -> followService.countFollowers(invalidId));
    verify(userRepository, times(1)).findById(invalidId);
  }

  @Test
  @DisplayName("팔로우 삭제 성공 시 followee의 팔로워 수 감소")
  void deleteFollowSuccess() {
    // given
    UUID followId = UUID.randomUUID();
    User followee = new User("followee@test.com", "1234", "followee", null, Role.USER);
    User follower = new User("follower@test.com", "1234", "follower", null, Role.USER);
    Follow follow = new Follow(followee, follower);
    follow.getFollowee().increaseFollowCount();

    when(followRepository.findById(followId)).thenReturn(Optional.of(follow));
    doNothing().when(followRepository).deleteById(followId);

    // when
    followService.delete(followId);

    // then
    assertEquals(0L, follow.getFollowee().getFollowCount());
    verify(followRepository, times(1)).findById(followId);
    verify(followRepository, times(1)).deleteById(followId);
  }

  @Test
  @DisplayName("팔로우 삭제 시 존재하지 않는 followId면 FollowNotFoundException 발생")
  void deleteFollowNotFound() {
    // given
    UUID followId = UUID.randomUUID();
    when(followRepository.findById(followId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(FollowNotFoundException.class, () -> followService.delete(followId));
    verify(followRepository, times(1)).findById(followId);
    verify(followRepository, never()).deleteById(any());
  }


}
