package com.codeit.playlist.domain.watching.event.publisher;

import com.codeit.playlist.domain.watching.dto.response.ContentChatDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SimpWatchingSessionPublisher implements WatchingSessionPublisher { // TODO: 카프카 구현체로 변경
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishWatching(UUID contentId, WatchingSessionChange event) {
        messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/watch", event);
    }

    @Override
    public void publishChat(UUID contentId, ContentChatDto contentChatDto) {
        messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", contentChatDto);
    }
}
