package com.codeit.playlist.domain.watching.event;

import com.codeit.playlist.domain.watching.dto.response.WatchingSessionChange;

import java.util.UUID;

public interface WatchingSessionPublisher {
    void publish(UUID contentId, WatchingSessionChange event);
}
