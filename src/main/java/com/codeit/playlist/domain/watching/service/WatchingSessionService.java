package com.codeit.playlist.domain.watching.service;

import com.codeit.playlist.domain.watching.dto.request.ContentChatSendRequest;

import java.util.UUID;

public interface WatchingSessionService {

    void watching(UUID contentId, UUID userId);

    long count(UUID contentId);

    void sendChat(UUID contentId, UUID userId, ContentChatSendRequest request);
}
