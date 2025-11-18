package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.exception.AlreadySubscribedException;
import com.codeit.playlist.domain.playlist.exception.NotSubscribedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
import com.codeit.playlist.domain.playlist.exception.SelfSubscriptionNotAllowedException;
import com.codeit.playlist.domain.playlist.exception.SubscriptionUpdateException;
import com.codeit.playlist.domain.playlist.repository.PlaylistRepository;
import com.codeit.playlist.domain.playlist.repository.SubscribeRepository;
import com.codeit.playlist.domain.playlist.service.basic.BasicPlaylistSubscriptionService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class BasicPlaylistSubscriptionServiceTest {
    @Mock
    PlaylistRepository playlistRepository;

    @Mock
    SubscribeRepository subscribeRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    BasicPlaylistSubscriptionService service;

    private static final UUID PLAYLIST_ID = UUID.randomUUID();
    private static final UUID SUBSCRIBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("구독 성공 - 로그인 유저가 아직 구독하지 않은 플레이리스트를 구독한다")
    void subscribeSuccess() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);
        User owner = mock(User.class);

        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(UUID.randomUUID());

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(false);
        given(playlistRepository.increaseSubscriberCount(PLAYLIST_ID))
                .willReturn(1);

        // when
        service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID);

        // then
        then(subscribeRepository).should()
                .save(any(Subscribe.class));
        then(playlistRepository).should()
                .increaseSubscriberCount(PLAYLIST_ID);
    }

    @Test
    @DisplayName("구독 실패 - 플레이리스트가 존재하지 않으면 PlaylistNotFoundException 발생")
    void subscribeFailWhenPlaylistNotFound() {
        // given
        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(PlaylistNotFoundException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(subscribeRepository).shouldHaveNoInteractions();
        then(playlistRepository).should(never()).increaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 실패 - 사용자(구독자)가 존재하지 않으면 UserNotFoundException 발생")
    void subscribeFailWhenUserNotFound() {
        // given
        Playlist playlist = mock(Playlist.class);
        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(UserNotFoundException.class);

        then(subscribeRepository).shouldHaveNoInteractions();
        then(playlistRepository).should(never()).increaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 실패 - 자기 자신의 플레이리스트는 구독할 수 없다")
    void subscribeFailWhenSelfSubscription() {
        // given
        Playlist playlist = mock(Playlist.class);
        User owner = mock(User.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(owner));

        // playlist.owner.id == subscriberId 인 상황
        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(SUBSCRIBER_ID);

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(SelfSubscriptionNotAllowedException.class);

        then(subscribeRepository).shouldHaveNoInteractions();
        then(playlistRepository).should(never()).increaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 실패 - 이미 구독 중이면 AlreadySubscribedException 발생")
    void subscribeFailWhenAlreadySubscribed() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);
        User owner = mock(User.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(UUID.randomUUID());
        given(subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(AlreadySubscribedException.class);

        then(subscribeRepository).should(never()).save(any());
        then(playlistRepository).should(never()).increaseSubscriberCount(any());;
    }

    @Test
    @DisplayName("구독 실패 - subscriberCount 증가에 실패하면 SubscriptionUpdateException 발생")
    void subscribeFailWhenSubscriptionCountUpdateFailed() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);
        User owner = mock(User.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(playlist.getOwner()).willReturn(owner);
        given(owner.getId()).willReturn(UUID.randomUUID()); // self 구독 아님
        given(subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(false);

        given(playlistRepository.increaseSubscriberCount(PLAYLIST_ID))
                .willReturn(0); // 업데이트 실패 상황

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(SubscriptionUpdateException.class);

        then(subscribeRepository).should().save(any(Subscribe.class));
        then(playlistRepository).should().increaseSubscriberCount(PLAYLIST_ID);
    }

    @Test
    @DisplayName("구독 해제 성공 - 구독 중인 플레이리스트를 정상적으로 해제한다")
    void unsubscribeSuccess() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);
        Subscribe subscribe = mock(Subscribe.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.findBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(Optional.of(subscribe));
        given(playlistRepository.decreaseSubscriberCount(PLAYLIST_ID))
                .willReturn(1);

        // when
        service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID);

        // then
        then(subscribeRepository).should().delete(subscribe);
        then(playlistRepository).should().decreaseSubscriberCount(PLAYLIST_ID);
    }

    @Test
    @DisplayName("구독 해제 실패 - 플레이리스트가 존재하지 않으면 PlaylistNotFoundException 발생")
    void unsubscribeFailWhenPlaylistNotFound() {
        // given
        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(PlaylistNotFoundException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(subscribeRepository).shouldHaveNoInteractions();
        then(playlistRepository).should(never()).decreaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 해제 실패 - 사용자(구독자)가 존재하지 않으면 UserNotFoundException 발생")
    void unsubscribeFailWhenUserNotFound() {
        // given
        Playlist playlist = mock(Playlist.class);
        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(UserNotFoundException.class);

        then(subscribeRepository).shouldHaveNoInteractions();
        then(playlistRepository).should(never()).decreaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 해제 실패 - 구독 중이 아니면 NotSubscribedException 발생")
    void unsubscribeFailWhenNotSubscribed() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.findBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(NotSubscribedException.class);

        then(subscribeRepository).should(never()).delete(any());
        then(playlistRepository).should(never()).decreaseSubscriberCount(any());
    }

    @Test
    @DisplayName("구독 해제 실패 - subscriberCount 감소에 실패하면 SubscriptionUpdateException 발생")
    void unsubscribeFailWhenSubscriptionCountUpdateFailed() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);
        Subscribe subscribe = mock(Subscribe.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.findBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(Optional.of(subscribe));
        given(playlistRepository.decreaseSubscriberCount(PLAYLIST_ID))
                .willReturn(0); // 감소 실패

        // when & then
        assertThatThrownBy(() -> service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(SubscriptionUpdateException.class);

        then(subscribeRepository).should().delete(subscribe);
        then(playlistRepository).should().decreaseSubscriberCount(PLAYLIST_ID);
    }
}
