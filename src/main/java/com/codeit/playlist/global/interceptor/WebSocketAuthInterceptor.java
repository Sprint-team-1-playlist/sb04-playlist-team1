package com.codeit.playlist.global.interceptor;

import com.codeit.playlist.domain.auth.exception.AuthHeaderMissingException;
import com.codeit.playlist.domain.auth.exception.InvalidOrExpiredException;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.security.jwt.JwtTokenProvider;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT == accessor.getCommand()) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                log.error("[WebSocket] 웹 소켓 인증을 위한 Authorization 헤더 없음");
                throw new AuthHeaderMissingException();
            }

            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            if (!jwtTokenProvider.validateAccessToken(token)) {
                log.error("[WebSocket] 유효하지 않은 토큰");
                throw InvalidOrExpiredException.withToken(token);
            }

            UUID userId = jwtTokenProvider.getUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> UserNotFoundException.withId(userId));
            PlaylistUserDetails userDetails = new PlaylistUserDetails(userMapper.toDto(user), null); // STOMP 에서 패스워드 사용하지 않으므로 null 처리

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            accessor.setUser(auth);
        }
        return message;
    }
}
