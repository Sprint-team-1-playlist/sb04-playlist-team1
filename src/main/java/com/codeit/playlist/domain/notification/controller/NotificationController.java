package com.codeit.playlist.domain.notification.controller;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;
import com.codeit.playlist.domain.notification.service.NotificationService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<CursorResponseNotificationDto> getNotificationList(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam SortDirection sortDirection,
            @RequestParam String sortBy,  //createdAt
            @AuthenticationPrincipal PlaylistUserDetails userDetails
            ) {
        UUID receiverId = userDetails.getUserDto().id();

        log.debug("[알림] 알림 목록 조회 요청 : receiverId= {}, cursor= {}, idAfter= {}, limit= {}, sortDirection= {}, sortBy= {}",
                receiverId, cursor, idAfter, limit, sortDirection, sortBy);

        CursorResponseNotificationDto response = notificationService.getAllNotifications(receiverId, cursor, idAfter,
                                                                                    limit, sortDirection, sortBy);

        log.info("[알림] 알림 목록 조회 성공 : receiverId= {}, 반환 개수= {}, hasNext= {}, totalCount= {}",
                receiverId, response.data() != null ? response.data().size() : 0,
                response.hasNext(), response.totalCount());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId,
                                                   @AuthenticationPrincipal PlaylistUserDetails userDetails) {
        log.debug("[알림] 알림 읽음 처리 시작 : notificationId= {}", notificationId);

        UUID currentUserId = userDetails.getUserDto().id();

        notificationService.markAsReadAndDeleteNotification(notificationId, currentUserId);

        log.info("[알림] 알림 읽음 처리 성공 : notificationId= {}", notificationId);

        return ResponseEntity.noContent().build();
    }
}
