package com.codeit.playlist.domain.auth.service.basic;

import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.JwtDto;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class BasicAuthService implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public UserDto updateRole(UserRoleUpdateRequest request, UUID userId) { // 권한 업데이트 로직, ADMIN 만 가능
    return updateRoleInternal(request, userId);
  }

  @Override
  public UserDto updateRoleInternal(UserRoleUpdateRequest request, UUID userId) {
    log.debug("[사용자 관리] 사용자 권한 변경 시작 : userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    Role oldRole = user.getRole();
    Role newRole = request.newRole();

    if (!oldRole.equals(newRole)) {
      user.updateRole(newRole);
      log.debug("[사용자 관리] 사용자 권한 변경 : userId={}, {} -> {}", userId, oldRole, newRole);
    }

    //JWT 토큰 확인하는 로직 필요
    // TODO: JWT 토큰 무효화 로직 구현
    // 역할이 변경되면 해당 사용자의 모든 JWT 토큰 무효화

    log.info("[사용자 관리] 사용자 권한 변경 완료 : userId={} , {} -> {}", userId, oldRole, newRole);

    return userMapper.toDto(user);
  }

  @Override
  public JwtDto signIn(String username, String password) throws JOSEException {

    //로그인 인증
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(username, password));

    SecurityContextHolder.getContext().setAuthentication(auth);

    //사용자 정보 조회
    PlaylistUserDetails userDetails = (PlaylistUserDetails) auth.getPrincipal();

    // JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(userDetails);

    UserDto userDto = userDetails.getUserDto();

    return new JwtDto(userDto, accessToken);
  }
}
