package com.codeit.playlist.domain.watching.service;

import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;

import java.util.UUID;

public interface WatchingSessionService {

    void joinWatching(String sessionId, UUID contentId, UUID userId);

    void leaveWatching(String sessionId);

    long count(UUID contentId);

    void sendChat(UUID contentId, UUID userId, ContentChatSendRequest request);
}
