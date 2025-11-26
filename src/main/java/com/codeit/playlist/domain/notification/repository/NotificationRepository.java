package com.codeit.playlist.domain.notification.repository;

import com.codeit.playlist.domain.notification.entity.Notification;
import com.codeit.playlist.domain.notification.repository.custom.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    long countByReceiver_Id(UUID receiverId);
}
