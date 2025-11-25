package com.codeit.playlist.global.interceptor;

import com.codeit.playlist.domain.auth.exception.AuthHeaderMissingException;
import com.codeit.playlist.domain.auth.exception.InvalidOrExpiredException;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.watching.fixture.WatchingSessionFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {
    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("CONNECT 시 Authorization 헤더가 없으면 AuthHeaderMissingException 발생")
    void missingAuthHeaderThrowsException() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AuthHeaderMissingException.class);
    }

    @Test
    @DisplayName("CONNECT 시 토큰이 유효하지 않으면 InvalidOrExpiredException 발생")
    void invalidTokenThrowsException() {
        // given
        String invalidToken = "invalid-token";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + invalidToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.validateAccessToken(invalidToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(InvalidOrExpiredException.class);
    }

    @Test
    @DisplayName("CONNECT 시 해당 유저를 DB 에서 찾지 못하면 UserNotFoundException 발생")
    void userNotFoundThrowsException() {
        // given
        String token = "valid-token";
        UUID userId = WatchingSessionFixtures.FIXED_ID;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.validateAccessToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("CONNECT 성공 시 accessor.setUser() 가 정상적으로 설정된다")
    void successfulConnectSetsAuthentication() {
        // given
        String token = "valid-token";
        UUID userId = WatchingSessionFixtures.FIXED_ID;
        User fakeUser = WatchingSessionFixtures.user();
        UserDto fakeUserDto = WatchingSessionFixtures.userDto();

        PlaylistUserDetails mappedDetails =
                new PlaylistUserDetails(fakeUserDto, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.validateAccessToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(userMapper.toDto(fakeUser)).thenReturn(fakeUserDto);

        // when
        Message<?> returned = interceptor.preSend(message, null);

        // then
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(returned);

        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(org.springframework.security.core.Authentication.class);

        verify(jwtTokenProvider).validateAccessToken(token);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(fakeUser);
    }
}