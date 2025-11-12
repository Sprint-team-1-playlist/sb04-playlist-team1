package com.codeit.playlist.domain.notification.exception;

import com.codeit.playlist.domain.conversation.exception.ConversationErrorCode;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;

import java.util.UUID;

public class NotificationNotFoundException extends NotificationException{

    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotificationNotFoundException withId(UUID notificationId) {
        NotificationNotFoundException exception = new NotificationNotFoundException();
        exception.addDetail("notificationId", notificationId);
        return exception;
    }
}
