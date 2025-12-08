package com.codeit.playlist.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.playlist.domain.user.entity.AuthProvider;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.config.QuerydslTestConfig;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@EnableJpaAuditing
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager em;

  private User createUser(String email) {
    return new User(
        "test@test.com",
        "encodedPw",
        "홍길동",
        null,
        Role.USER
    );
  }

  @Test
  @DisplayName("existsByEmail - 이메일 존재 여부 확인")
  void existsByEmail() {
    // given
    User user = userRepository.save(createUser("test@test.com"));

    // when
    boolean exists = userRepository.existsByEmail("test@test.com");

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("findByEmail - 이메일로 사용자 조회")
  void findByEmail() {
    // given
    User user = userRepository.save(createUser("test@test.com"));

    // when
    Optional<User> result = userRepository.findByEmail("test@test.com");

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("test@test.com");
  }

  @Test
  @DisplayName("changePassword - 비밀번호 변경")
  void changePassword() {
    // given
    User user = userRepository.save(createUser("test@test.com"));
    UUID userId = user.getId();

    // when
    userRepository.changePassword(userId, "newEncodedPw");

    em.flush();
    em.clear();

    // then
    User updated =
        userRepository.findById(userId).orElseThrow();

    assertThat(updated.getPassword()).isEqualTo("newEncodedPw");
  }

  @Test
  @DisplayName("updateUserLocked - 사용자 잠금 상태 변경")
  void updateUserLocked() {
    // given
    User user = userRepository.save(createUser("test@test.com"));
    UUID userId = user.getId();

    // when
    userRepository.updateUserLocked(userId, true);

    em.flush();
    em.clear();

    // then
    User updated =
        userRepository.findById(userId).orElseThrow();

    assertThat(updated.isLocked()).isTrue();
  }

  @Test
  void findByEmailAndProvider() {
    AuthProvider provider = AuthProvider.LOCAL;

    // given
    User user = new User(
        "test@test.com",
        "encodedPw",
        "홍길동",
        null,
        Role.USER
    );

    userRepository.saveAndFlush(user);

    Optional<User> result =
        userRepository.findByEmailAndProvider("test@test.com", provider);

    assertThat(result).isPresent();
  }
}