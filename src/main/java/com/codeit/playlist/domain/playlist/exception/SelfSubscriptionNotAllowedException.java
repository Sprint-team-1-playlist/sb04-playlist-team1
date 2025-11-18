package com.codeit.playlist.domain.playlist.exception;

import java.util.UUID;

public class SelfSubscriptionNotAllowedException extends PlaylistException {
    public SelfSubscriptionNotAllowedException() {
        super(PlaylistErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);
    }

    public static SelfSubscriptionNotAllowedException withDetail(UUID playlistId, UUID subscriptionId) {
        SelfSubscriptionNotAllowedException ex = new SelfSubscriptionNotAllowedException();
        ex.addDetail("playlistId", playlistId);
        ex.addDetail("userId", subscriptionId);
        return ex;
    }
}
