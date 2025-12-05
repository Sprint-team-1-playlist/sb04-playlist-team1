package com.codeit.playlist.domain.watching.event.publisher;

import com.codeit.playlist.domain.watching.dto.response.ContentChatDto;
import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;

import java.util.UUID;

public interface WatchingSessionPublisher {
    void publishWatching(UUID contentId, WatchingSessionChange event);

    void publishChat(UUID contentId, ContentChatDto contentChatDto);
}
