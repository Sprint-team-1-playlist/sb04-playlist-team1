package com.codeit.playlist.domain.playlist.service.basic;

import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.exception.AlreadySubscribedException;
import com.codeit.playlist.domain.playlist.exception.NotSubscribedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.exception.SelfSubscriptionNotAllowedException;
import com.codeit.playlist.domain.playlist.exception.SubscriptionUpdateException;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.service.PlaylistSubscriptionService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BasicPlaylistSubscriptionService implements PlaylistSubscriptionService {

    private final PlaylistRepository playlistRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void subscribe(UUID playlistId, UUID currentUserId) {

        log.debug("[구독] 플레이리스트 구독 실행 : playlistId= {}, currentUserId= {}", playlistId, currentUserId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        User subscriber = userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.withId(currentUserId));

        // 자기 자신이 만든 플레이리스트는 구독할 수 없음
        if (playlist.getOwner().getId().equals(currentUserId)) {
            log.error("[구독] 자기 자신의 플레이리스트는 구독할 수 없습니다. playlistId= {}, ownerId= {}, currentUserId= {}",
                    playlistId, playlist.getOwner().getId(), currentUserId);
            throw SelfSubscriptionNotAllowedException.withDetail(playlistId, currentUserId);
        }

        // 이미 구독 중이면 예외
        if (subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist)) {
            log.error("[구독] 이미 구독한 플레이리스트입니다. playlistId= {}, currentUserId= {}",
                    playlistId, currentUserId);
            throw AlreadySubscribedException.withDetail(playlistId, currentUserId);
        }

        Subscribe subscribe = new Subscribe(subscriber, playlist);
        subscribeRepository.save(subscribe);

        int increased = playlistRepository.increaseSubscriberCount(playlistId);
        if (increased == 0) {
            log.error("[구독] subscriberCount 증가 실패 : playlistId= {}", playlistId);
            throw SubscriptionUpdateException.withId(playlistId);
        }

        UUID ownerId = playlist.getOwner().getId();

        String title = String.format("[%s]님이 내 플레이리스트를 구독하였습니다", subscriber.getName());
        String contentMsg = String.format("[ %s ] %s", playlist.getTitle(), playlist.getDescription());

        NotificationDto notificationDto = new NotificationDto(null, null, ownerId,
                                                                title, contentMsg, Level.INFO);

        try {
            String payload = objectMapper.writeValueAsString(notificationDto);
            kafkaTemplate.send("playlist.NotificationDto", payload);

            log.info("[구독] Playlist 구독 알림 이벤트 발행 완료 : ownerId= {}, subscriberId= {}",
                    ownerId, subscriber.getId());
        } catch (JsonProcessingException e) {
            log.error("[구독] Playlist 구독 알림 이벤트 직렬화 실패 : ownerId= {}, subscriberId= {}",
                    ownerId, subscriber.getId(), e);
        }

        log.info("[구독] 플레이리스트 구독 성공 : playlistId= {}, currentUserId= {}", playlistId, currentUserId);
    }

    @Override
    public void unsubscribe(UUID playlistId, UUID currentUserId) {
        log.debug("[구독] 구독 해제 실행 : playlistId = {}, currentUserId = {}", playlistId, currentUserId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> PlaylistNotFoundException.withId(playlistId));

        User subscriber = userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.withId(currentUserId));

        Subscribe subscribe = subscribeRepository
                .findBySubscriberAndPlaylist(subscriber, playlist)
                .orElseThrow(() -> NotSubscribedException.withDetail(playlistId, currentUserId));

        subscribeRepository.delete(subscribe);

        int decreased = playlistRepository.decreaseSubscriberCount(playlistId);
        if (decreased == 0) {
            log.error("[구독] subscriberCount 감소 실패 : playlistId= {}", playlistId);
            throw SubscriptionUpdateException.withId(playlistId);
        }

        log.info("[구독] 구독 해제 성공 : playlistId= {}, subscriberId= {}", playlistId, currentUserId);
    }
}
