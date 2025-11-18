package com.codeit.playlist.domain.watching.service;

import java.util.UUID;

public interface WatchingSessionService {

    void join(UUID contentId);

    void leave(UUID contentId);
}
