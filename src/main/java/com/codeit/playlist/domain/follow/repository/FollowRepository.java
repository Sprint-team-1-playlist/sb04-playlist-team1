package com.codeit.playlist.domain.follow.repository;

import com.codeit.playlist.domain.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  boolean existsByFollowerIdAndFolloweeId(UUID testFollowerId, UUID id);
}
