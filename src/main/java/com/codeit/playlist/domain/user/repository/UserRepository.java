package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  Optional<User> findByName(String username);

  Optional<User> findByEmail(String email);

  @Modifying
  @Transactional
  @Query("UPDATE User u SET u.password = :newPassword WHERE u.id = :userId")
  void updatedPassword(@Param("userId") UUID userId, @Param("newPassword") String newPassword);
}
