package com.codeit.playlist.domain.watching.exception;

import java.util.UUID;

public class JsonSerializationFailedException extends WatchingException {
    public JsonSerializationFailedException() {
        super(WatchingErrorCode.JSON_SERIALIZATION_FAILED);
    }

    public static JsonSerializationFailedException withContentIdAndUserId(UUID contentId, UUID userId) {
        JsonSerializationFailedException exception = new JsonSerializationFailedException();
        exception.addDetail("contentId", contentId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
