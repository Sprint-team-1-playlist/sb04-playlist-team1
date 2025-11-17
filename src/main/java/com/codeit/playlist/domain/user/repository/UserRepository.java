package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  Optional<User> findByName(String username);

  Optional<User> findByEmail(String email);

  void updatedPassword(UUID userId, ChangePasswordRequest request);
}
