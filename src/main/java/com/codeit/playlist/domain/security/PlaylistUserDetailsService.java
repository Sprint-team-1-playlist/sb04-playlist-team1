package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserLockStateUnchangedException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("[인증 관리] : 유저의 이름을 불러오고 있습니다.");
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

    if(user.isLocked()){
      throw UserLockStateUnchangedException.withId(user.getId());
    }
    UserDto userDto = userMapper.toDto(user);

    log.debug("[인증 관리] : 유저를 불러오는 데 성공했습니다."); // 모든 인증된 요청마다 호출되므로 과도한 로그 볼륨을 생성시킬 수 있음. 그래서 debug 로 레벨 격하
    return new PlaylistUserDetails(
        userDto,
        user.getPassword()
    );
  }
}
