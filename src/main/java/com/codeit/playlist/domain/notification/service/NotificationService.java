package com.codeit.playlist.domain.notification.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.dto.data.NotificationSortBy;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;

import java.util.UUID;

public interface NotificationService {

    NotificationDto createNotification(UUID receiverId, String title, String content, Level level);

    NotificationDto saveNotification(UUID receiverId, String title, String content, Level level);

    CursorResponseNotificationDto getAllNotifications(UUID receiverId, String cursor, UUID idAfter,
                                                      int limit, SortDirection sortDirection, NotificationSortBy sortBy);

    void markAsReadAndDeleteNotification(UUID notificationId, UUID currentUserId);
}
