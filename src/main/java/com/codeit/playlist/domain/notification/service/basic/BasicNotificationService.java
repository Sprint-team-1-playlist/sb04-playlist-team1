package com.codeit.playlist.domain.notification.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;
import com.codeit.playlist.domain.notification.entity.Notification;
import com.codeit.playlist.domain.notification.exception.NotificationNotFoundException;
import com.codeit.playlist.domain.notification.exception.NotificationReadDeniedException;
import com.codeit.playlist.domain.notification.mapper.NotificationMapper;
import com.codeit.playlist.domain.notification.repository.NotificationRepository;
import com.codeit.playlist.domain.notification.service.NotificationService;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.codeit.playlist.global.error.InvalidSortByException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    //알림 목록 조회
    @Transactional(readOnly = true)
    @Override
    public CursorResponseNotificationDto getAllNotifications(UUID receiverId, String cursor, UUID idAfter,
                                                             int limit, SortDirection sortDirection, String sortBy) {

        log.debug("[알림] 알림 목록 조회 서비스 시작: receiverId= {}, cursor= {}, idAfter= {}, limit= {}, sortDirection= {}, sortBy= {}",
                receiverId, cursor, idAfter, limit, sortDirection, sortBy);

        if (limit <= 0 || limit > 50) {
            limit = 10;
        }

        SortDirection safeSortDirection =
                (sortDirection == null) ? SortDirection.DESCENDING : sortDirection;

        //sortBy 보정 + 검증 (createdAt만 허용)
        String safeSortBy = (sortBy == null || sortBy.isBlank())
                ? "createdAt" : sortBy;

        if (!safeSortBy.equals("createdAt")) {
            throw InvalidSortByException.withSortBy(sortBy);
        }

        //cursor 파싱
        UUID cursorId = null;

        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorId = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                throw InvalidCursorException.withCursor(cursor);
            }
        }

        if (cursorId == null && idAfter != null) {
            cursorId = idAfter;
        }

        //정렬 정보 설정 (createdAt + id 보조 정렬)
        Sort sort = (safeSortDirection == SortDirection.ASCENDING)
                ? Sort.by(Sort.Order.asc(safeSortBy), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc(safeSortBy), Sort.Order.desc("id"));

        //Pageable 설정
        Pageable pageable = PageRequest.of(0, limit, sort);

        //Repository 호출 (limit+1 조회)
        Slice<Notification> slice =
                notificationRepository.findByReceiverIdWithCursorPaging(
                        receiverId,
                        cursorId,
                        pageable
                );

        List<Notification> notifications = slice.getContent();

        //엔티티 → DTO
        List<NotificationDto> content = notifications.stream()
                .map(notificationMapper::toDto).toList();

        //nextCursor, nextIdAfter 계산 (id 기준 커서)
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (slice.hasNext() && !notifications.isEmpty()) {
            Notification last = notifications.get(notifications.size() - 1);
            nextCursor = last.getId().toString();   // id를 커서로 사용
            nextIdAfter = last.getId();
        }

        Long totalCount = null;
        if (cursorId == null) {
            totalCount = notificationRepository.countByReceiver_Id(receiverId);
        }

        CursorResponseNotificationDto response = new CursorResponseNotificationDto(
                content,
                nextCursor,
                nextIdAfter,
                slice.hasNext(),
                totalCount,
                safeSortBy,
                safeSortDirection
        );

        log.info("[알림] 알림 목록 조회 완료: receiverId={}, count={}, hasNext={}",
                receiverId, content.size(), slice.hasNext());

        return response;
    }

    //알림 읽음 처리(하드 딜리트)
    @Transactional
    @Override
    public void markAsReadAndDeleteNotification(UUID notificationId, UUID currentUserId) {

        log.debug("[알림] 알림 읽음 처리 시작 : notificationId= {}, currentUserId= {}", notificationId, currentUserId);

        //알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.error("알림이 존재하지 않습니다. : notificationId= {}", notificationId);
                    return NotificationNotFoundException.withId(notificationId);
                });

        //권한 검증
        if (!notification.getReceiver().getId().equals(currentUserId)) {
            log.error("본인의 알림이 아닙니다. : notificationId= {}, currentUserId= {}", notificationId, currentUserId);
            throw NotificationReadDeniedException.withId(notificationId);
        }

        log.info("[알림] 읽음(삭제) 진행: notificationId= {}, currentUserId= {}, title= {}, content= {}",
                notificationId, currentUserId, notification.getTitle(), notification.getContent());

        notificationRepository.delete(notification);

        log.info("[알림] 알림 읽음 처리 성공 : notificationId= {}, currentUserId= {}", notificationId, currentUserId);
    }

}
