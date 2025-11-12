package com.codeit.playlist.domain.user.service.basic;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.EmailAlreadyExistsException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
//  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;


  @Override
  public UserDto registerUser (UserCreateRequest request){
    log.debug("[사용자 관리] 사용자 등록 시작 : email = {}", request.email());

    User newUser = userMapper.toEntity(request);

    if(userRepository.existsByEmail(newUser.getEmail())) {
      throw new EmailAlreadyExistsException();
    }

    // passwordEncoder 로 암호화 해야함 추후 적용 예정
    // String encodedPassword = passwordEncoder.encode(newUser.getPassword);
    //newUser.updatePassword(encodedPassword)

    log.info("[사용자 관리] 사용자 등록 완료 : email = {}", request.email());

    return userMapper.toDto(userRepository.save(newUser));
  }

}
