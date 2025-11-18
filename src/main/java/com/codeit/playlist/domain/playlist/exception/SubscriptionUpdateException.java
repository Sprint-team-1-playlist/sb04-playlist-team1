package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class SubscriptionUpdateException extends PlaylistException {
    public SubscriptionUpdateException(UUID playlistId) {
        super(PlaylistErrorCode.SUBSCRIBER_COUNT_UPDATE_FAILED);
        addDetail("playlistId", playlistId);
    }

  public static SubscriptionUpdateException withId(UUID playlistId) {
    return new SubscriptionUpdateException(playlistId);
  }
}
