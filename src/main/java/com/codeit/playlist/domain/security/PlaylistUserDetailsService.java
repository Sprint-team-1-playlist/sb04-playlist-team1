package com.codeit.playlist.domain.security;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
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
    log.debug("[인증 관리] : 유저의 이름을 불러오고 있습니다. : {}", username);
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    UserDto userDto = userMapper.toDto(user);


    log.info("유저를 불러오는데에 성공했습니다.: {}", username);
    return new PlaylistUserDetails(
        userDto,
        user.getPassword()
    );
  }
}
