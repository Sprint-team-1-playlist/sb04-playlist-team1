package com.codeit.playlist.domain.notification.exception;

import java.util.UUID;

public class NotificationReadDeniedException extends NotificationException {
    public NotificationReadDeniedException() {
        super(NotificationErrorCode.NOTIFICATION_READ_DENIED);
    }

    public static NotificationReadDeniedException withId(UUID notificationId) {
        NotificationReadDeniedException exception = new NotificationReadDeniedException();
        exception.addDetail("notificationId", notificationId);
        return exception;
    }
}
