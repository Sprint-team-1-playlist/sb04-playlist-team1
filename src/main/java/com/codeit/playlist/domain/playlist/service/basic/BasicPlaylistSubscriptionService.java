package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.exception.AlreadySubscribedException;
import com.codeit.playlist.domain.playlist.exception.NotSubscribedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistSubscriptionService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPlaylistSubscriptionService implements PlaylistSubscriptionService {

    private final PlaylistRepository playlistRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public void subscribe(UUID playlistId, UUID subscriberId) {

        log.debug("[구독] 시작 : playlistId={}, subscriberId={}", playlistId, subscriberId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(PlaylistNotFoundException::new);

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(UserNotFoundException::new);

        // 이미 구독 중이면 예외
        if (subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist)) {
            throw AlreadySubscribedException.withDetail(playlistId, subscriberId);
        }

        Subscribe subscribe = new Subscribe(subscriber, playlist);
        subscribeRepository.save(subscribe);

        playlist.increaseSubscriberCount();

        log.info("[구독] 성공 : playlistId={}, subscriberId={}", playlistId, subscriberId);
    }

    @Transactional
    @Override
    public void unsubscribe(UUID playlistId, UUID subscriberId) {
        log.debug("[구독] 구독 해제 시작 : playlistId={}, subscriberId={}", playlistId, subscriberId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(PlaylistNotFoundException::new);

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(UserNotFoundException::new);

        Subscribe subscribe = subscribeRepository
                .findBySubscriberAndPlaylist(subscriber, playlist)
                .orElseThrow(() -> NotSubscribedException.withDetail(playlistId, subscriberId));

        subscribeRepository.delete(subscribe);
        playlist.decreaseSubscriberCount();

        log.info("[구독] 구독 해제 성공 : playlistId={}, subscriberId={}", playlistId, subscriberId);
    }
}
