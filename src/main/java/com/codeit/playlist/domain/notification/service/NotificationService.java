package com.codeit.playlist.domain.notification.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;

import java.util.UUID;

public interface NotificationService {
    CursorResponseNotificationDto getAllNotifications(UUID receiverId, String cursor, UUID idAfter,
                                                      int limit, SortDirection sortDirection, String sortBy);

    void markAsReadAndDeleteNotification(UUID notificationId, UUID currentUserId);
}
