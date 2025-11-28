package com.codeit.playlist.domain.follow.repository;

import com.codeit.playlist.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  //특정 사용자를 팔로우하는 팔로워들의 목록을 조회
  @Query("select f.follower.id " +
          "from Follow f " +
          "where f.followee.id = :followeeId")
  List<UUID> findFollowerIdsByFolloweeId(@Param("followeeId")UUID followeeId);
}
