package com.codeit.playlist.domain.watching.event.impl;

import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;
import com.codeit.playlist.domain.watching.event.WatchingSessionPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SimpWatchingSessionPublisher implements WatchingSessionPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(UUID contentId, WatchingSessionChange event) {
        messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/watch", event);
    }
}
