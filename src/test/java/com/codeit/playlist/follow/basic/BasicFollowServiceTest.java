package com.codeit.playlist.follow.basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.codeit.playlist.domain.follow.dto.data.FollowDto;
import com.codeit.playlist.domain.follow.dto.request.FollowRequest;
import com.codeit.playlist.domain.follow.entity.Follow;
import com.codeit.playlist.domain.follow.exception.FollowAlreadyExistsException;
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

    follower = new User("follower@test.com", "1234", "follower", null, Role.USER, false, 0L);
    followee = new User("followee@test.com", "1234", "followee", null, Role.USER, false, 0L);

    follow = new Follow(followee, follower);
  }

  @Test
  @DisplayName("팔로우 생성 성공")
  void createFollow_Success() {
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
  void createFollow_FolloweeNotFound() {
    // given: userRepository에서 followee를 찾을 수 없도록 설정
    when(userRepository.findById(followeeId)).thenReturn(Optional.empty());

    // when then: UserNotFoundException이 발생해야 함
    assertThrows(UserNotFoundException.class, () -> followService.create(followRequest));

    verify(userRepository, times(1)).findById(followeeId);
  }

  @Test
  @DisplayName("자기 자신을 팔로우할 경우 400 예외 발생")
  void createFollow_SelfFollowNotAllowed() {
    FollowRequest selfRequest = new FollowRequest(followerId);

    assertThrows(FollowSelfNotAllowedException.class, () -> followService.create(selfRequest));
  }

  @Test
  @DisplayName("이미 팔로우 중일 경우 400 예외 발생")
  void createFollow_AlreadyFollowing() {
    when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));
    when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

    assertThrows(FollowAlreadyExistsException.class, () -> followService.create(followRequest));
  }
}
