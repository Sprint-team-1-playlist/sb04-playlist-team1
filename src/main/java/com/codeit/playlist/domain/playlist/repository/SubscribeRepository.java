package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscribeRepository extends JpaRepository<Subscribe, UUID> {
    boolean existsBySubscriberAndPlaylist(User subscriber, Playlist playlist);

    boolean existsBySubscriber_IdAndPlaylist_Id(UUID subscriber, UUID playlistId);

    Optional<Subscribe> findBySubscriberAndPlaylist(User subscriber, Playlist playlist);

    //플레이리스트의 구독자를 조회
    @Query("select distinct s.subscriber.id " +
            "from Subscribe s " +
            "where s.playlist.id = :playlistId")
    List<UUID> findSubscriberIdByPlaylistId(@Param("playlistId") UUID playlistId);
}
