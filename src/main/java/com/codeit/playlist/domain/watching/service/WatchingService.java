package com.codeit.playlist.domain.watching.service;

import com.codeit.playlist.domain.watching.dto.request.WatchingSessionRequest;
import com.codeit.playlist.domain.watching.dto.response.CursorResponseWatchingSessionDto;

import java.util.UUID;

public interface WatchingService {
    public CursorResponseWatchingSessionDto getWatchingSessions(UUID contentId, WatchingSessionRequest request);
}
