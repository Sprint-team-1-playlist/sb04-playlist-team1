package com.codeit.playlist.domain.notification.repository.custom;

import com.codeit.playlist.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface NotificationRepositoryCustom {

    //알림 목록 커서 조회
    Slice<Notification> findByReceiverIdWithCursorPaging(
            UUID receiverId, UUID cursorId, Pageable pageable
    );
}
