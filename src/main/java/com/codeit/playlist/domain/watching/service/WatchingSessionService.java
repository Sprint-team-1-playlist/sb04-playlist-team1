package com.codeit.playlist.domain.watching.service;

import java.util.UUID;

public interface WatchingSessionService {

    void join(UUID contentId, UUID userId);

    void leave(UUID contentId, UUID userId);

    long count(UUID contentId);
}
