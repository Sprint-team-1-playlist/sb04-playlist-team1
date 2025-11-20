package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

    boolean existsByPlaylist_IdAndContent_Id(UUID playlistId, UUID contentId);

    Optional<PlaylistContent> findByPlaylist_IdAndContent_Id(UUID playlistId, UUID contentId);
}
