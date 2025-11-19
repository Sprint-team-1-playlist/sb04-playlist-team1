package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscribeRepository extends JpaRepository<Subscribe, UUID> {
    boolean existsBySubscriberAndPlaylist(User subscriber, Playlist playlist);

    Optional<Subscribe> findBySubscriberAndPlaylist(User subscriber, Playlist playlist);
}
