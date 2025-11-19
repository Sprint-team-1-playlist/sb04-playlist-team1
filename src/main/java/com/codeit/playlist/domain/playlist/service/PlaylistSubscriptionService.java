package com.codeit.playlist.domain.playlist.service;

import java.util.UUID;

public interface PlaylistSubscriptionService {
    void subscribe(UUID playlistId, UUID subscriberId);

    void unsubscribe(UUID playlistId, UUID subscriberId);
}
