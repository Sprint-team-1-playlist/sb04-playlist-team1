package com.codeit.playlist.playlist.service.basic;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.Subscribe;
import com.codeit.playlist.domain.playlist.exception.AlreadySubscribedException;
import com.codeit.playlist.domain.playlist.exception.NotSubscribedException;
import com.codeit.playlist.domain.playlist.exception.PlaylistNotFoundException;
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

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(false);

        // when
        service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID);

        // then
        then(subscribeRepository).should()
                .save(any(Subscribe.class));
        then(playlist).should()
                .increaseSubscriberCount();
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
    }

    @Test
    @DisplayName("구독 실패 - 이미 구독 중이면 AlreadySubscribedException 발생")
    void subscribeFailWhenAlreadySubscribed() {
        // given
        Playlist playlist = mock(Playlist.class);
        User subscriber = mock(User.class);

        given(playlistRepository.findById(PLAYLIST_ID))
                .willReturn(Optional.of(playlist));
        given(userRepository.findById(SUBSCRIBER_ID))
                .willReturn(Optional.of(subscriber));
        given(subscribeRepository.existsBySubscriberAndPlaylist(subscriber, playlist))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.subscribe(PLAYLIST_ID, SUBSCRIBER_ID))
                .isInstanceOf(AlreadySubscribedException.class);

        then(subscribeRepository).should(never()).save(any());
        then(playlist).should(never()).increaseSubscriberCount();
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

        // when
        service.unsubscribe(PLAYLIST_ID, SUBSCRIBER_ID);

        // then
        then(subscribeRepository).should().delete(subscribe);
        then(playlist).should().decreaseSubscriberCount();
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
        then(playlist).should(never()).decreaseSubscriberCount();
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
        then(playlist).should(never()).decreaseSubscriberCount();
    }
}
