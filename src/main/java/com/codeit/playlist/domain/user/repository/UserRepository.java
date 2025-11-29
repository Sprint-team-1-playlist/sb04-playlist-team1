package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  boolean existsByName(String username);

  Optional<User> findByName(String username);

  Optional<User> findByEmail(String email);

  @Modifying
  @Transactional
  @Query("UPDATE User u SET u.password = :newPassword WHERE u.id = :userId")
  void changePassword(@Param("userId") UUID userId, @Param("newPassword") String newPassword);

  @Modifying
  @Transactional
  @Query("update User u set u.locked = :locked where u.id = :userId")
  void updateUserLocked(@Param("userId") UUID userId, @Param("locked") boolean locked);
}
