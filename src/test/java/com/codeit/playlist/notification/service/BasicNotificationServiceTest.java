package com.codeit.playlist.notification.service;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.dto.data.NotificationSortBy;
import com.codeit.playlist.domain.notification.dto.response.CursorResponseNotificationDto;
import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.notification.entity.Notification;
import com.codeit.playlist.domain.notification.exception.NotificationNotFoundException;
import com.codeit.playlist.domain.notification.exception.NotificationReadDeniedException;
import com.codeit.playlist.domain.notification.mapper.NotificationMapper;
import com.codeit.playlist.domain.notification.repository.NotificationRepository;
import com.codeit.playlist.domain.notification.service.basic.BasicNotificationService;
import com.codeit.playlist.domain.sse.service.SseService;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.error.InvalidCursorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class BasicNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseService sseService;

    @InjectMocks
    private BasicNotificationService notificationService;

    @Test
    @DisplayName("알림 생성 성공 - 유저 조회, 알림 저장, DTO 변환, SSE 전송까지 정상 수행")
    void createNotificationSuccess() {
        //given
        UUID receiverId = UUID.randomUUID();
        String title = "새 구독";
        String content = "누가 내 플레이리스트를 구독했습니다.";
        Level level = Level.INFO;

        User receiver = Mockito.mock(User.class);
        Notification notification = Mockito.mock(Notification.class);
        Notification savedNotification = Mockito.mock(Notification.class);

        NotificationDto dto = new NotificationDto(UUID.randomUUID(), LocalDateTime.now(), receiverId,
                                                    title, content, level);

        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);
        given(notificationMapper.toDto(savedNotification)).willReturn(dto);

        //when
        NotificationDto result = notificationService.createNotification(receiverId, title, content, level);

        //then
        assertThat(result).isEqualTo(dto);

        then(userRepository).should().findById(receiverId);
        then(notificationRepository).should().save(any(Notification.class));
        then(notificationMapper).should().toDto(savedNotification);
        then(sseService).should().send(eq(List.of(receiverId)), eq("notifications"), eq(dto));
    }

    @Test
    @DisplayName("알림 생성 실패 - 수신자 조회 실패 시 UserNotFoundException 발생")
    void createNotificationFailWithUserNotFound() {
        // given
        UUID receiverId = UUID.randomUUID();
        String title = "새 구독";
        String content = "내용";
        Level level = Level.INFO;

        given(userRepository.findById(receiverId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationService.createNotification(receiverId, title, content, level)
        )
                .isInstanceOf(UserNotFoundException.class);

        then(userRepository).should().findById(receiverId);
        then(notificationRepository).shouldHaveNoInteractions();
        then(notificationMapper).shouldHaveNoInteractions();
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 생성 실패 - 알림 저장 중 예외 발생 시 그대로 전파")
    void createNotificationFailWithSaveError() {
        // given
        UUID receiverId = UUID.randomUUID();
        String title = "새 구독";
        String content = "내용";
        Level level = Level.INFO;

        User receiver = Mockito.mock(User.class);

        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(notificationRepository.save(any(Notification.class)))
                .willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() ->
                notificationService.createNotification(receiverId, title, content, level)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        then(userRepository).should().findById(receiverId);
        then(notificationRepository).should().save(any(Notification.class));
        then(notificationMapper).shouldHaveNoInteractions();
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 생성 실패 - DTO 변환 중 예외 발생 시 그대로 전파되고 SSE는 호출되지 않음")
    void createNotificationFailWithMappingError() {
        // given
        UUID receiverId = UUID.randomUUID();
        String title = "새 구독";
        String content = "내용";
        Level level = Level.INFO;

        User receiver = Mockito.mock(User.class);
        Notification saved = Mockito.mock(Notification.class);

        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(notificationRepository.save(any(Notification.class))).willReturn(saved);
        given(notificationMapper.toDto(saved))
                .willThrow(new RuntimeException("Mapping error"));

        // when & then
        assertThatThrownBy(() ->
                notificationService.createNotification(receiverId, title, content, level)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mapping error");

        then(userRepository).should().findById(receiverId);
        then(notificationRepository).should().save(any(Notification.class));
        then(notificationMapper).should().toDto(saved);
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 커서 페이지네이션과 totalCount, nextCursor, nextIdAfter 정상 반환")
    void getAllNotificationsSuccess() {
        // given
        UUID receiverId = UUID.randomUUID();
        String cursor = null;
        UUID idAfter = null;
        int limit = 10;
        SortDirection sortDirection = SortDirection.DESCENDING;
        NotificationSortBy sortBy = NotificationSortBy.createdAt;

        Notification notification1 = Mockito.mock(Notification.class);
        Notification notification2 = Mockito.mock(Notification.class);

        UUID id2 = UUID.randomUUID();
        LocalDateTime createdAt2 = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        given(notification2.getId()).willReturn(id2);
        given(notification2.getCreatedAt()).willReturn(createdAt2);

        List<Notification> entities = List.of(notification1, notification2);

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Slice<Notification> slice = new SliceImpl<>(entities, pageable, true);

        given(notificationRepository.findByReceiverIdWithCursorPaging(
                eq(receiverId),
                isNull(LocalDateTime.class),
                isNull(UUID.class),
                any(Pageable.class)
        )).willReturn(slice);

        given(notificationRepository.countByReceiver_Id(receiverId))
                .willReturn(2L);

        NotificationDto dto1 = Mockito.mock(NotificationDto.class);
        NotificationDto dto2 = Mockito.mock(NotificationDto.class);

        given(notificationMapper.toDto(notification1)).willReturn(dto1);
        given(notificationMapper.toDto(notification2)).willReturn(dto2);

        String expectedNextCursor = createdAt2.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // when
        CursorResponseNotificationDto response = notificationService.getAllNotifications(
                receiverId,
                cursor,
                idAfter,
                limit,
                sortDirection,
                sortBy
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.data()).hasSize(2)
                .containsExactly(dto1, dto2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo(SortDirection.DESCENDING);
        assertThat(response.nextCursor()).isEqualTo(expectedNextCursor);
        assertThat(response.nextIdAfter()).isEqualTo(id2);

        then(notificationRepository).should().findByReceiverIdWithCursorPaging(
                eq(receiverId),
                isNull(LocalDateTime.class),
                isNull(UUID.class),
                any(Pageable.class)
        );
        then(notificationRepository).should().countByReceiver_Id(receiverId);
        then(notificationMapper).should().toDto(notification1);
        then(notificationMapper).should().toDto(notification2);
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 잘못된 cursor 문자열로 InvalidCursorException 발생")
    void getAllNotificationsFailWithInvalidCursorFormat() {
        // given
        UUID receiverId = UUID.randomUUID();
        String cursor = "not-a-datetime";
        UUID idAfter = null;
        int limit = 10;
        SortDirection sortDirection = SortDirection.DESCENDING;
        NotificationSortBy sortBy = NotificationSortBy.createdAt;

        // when & then
        assertThatThrownBy(() ->
                notificationService.getAllNotifications(
                        receiverId,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy
                )
        )
                .isInstanceOf(InvalidCursorException.class)
                .hasMessageContaining("cursor는 'yyyy-MM-ddTHH:mm:ss' 형식이어야 합니다.");

        then(notificationRepository).shouldHaveNoInteractions();
        then(notificationMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - Repository 예외 발생 시 그대로 전파 (RuntimeException)")
    void getAllNotificationsFailWhenRepositoryThrowsException() {
        // given
        UUID receiverId = UUID.randomUUID();
        String cursor = null;
        UUID idAfter = null;
        int limit = 10;
        SortDirection sortDirection = SortDirection.DESCENDING;
        NotificationSortBy sortBy = NotificationSortBy.createdAt;

        given(notificationRepository.findByReceiverIdWithCursorPaging(
                eq(receiverId),
                isNull(LocalDateTime.class),
                isNull(UUID.class),
                any(Pageable.class)
        )).willThrow(new RuntimeException("DB error"));

        // when & then
        assertThatThrownBy(() ->
                notificationService.getAllNotifications(
                        receiverId,
                        cursor,
                        idAfter,
                        limit,
                        sortDirection,
                        sortBy
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        then(notificationRepository).should().findByReceiverIdWithCursorPaging(
                eq(receiverId),
                isNull(LocalDateTime.class),
                isNull(UUID.class),
                any(Pageable.class)
        );

        then(notificationRepository).should(never()).countByReceiver_Id(any());
        then(notificationMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리 성공 - 본인 알림 삭제 성공")
    void markAsReadAndDeleteNotificationSuccess() {
        //given
        UUID notificationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User receiver = Mockito.mock(User.class);
        Notification notification = Mockito.mock(Notification.class);

        given(notification.getReceiver()).willReturn(receiver);
        given(receiver.getId()).willReturn(currentUserId);

        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.of(notification));

        //when
        notificationService.markAsReadAndDeleteNotification(notificationId, currentUserId);

        //then
        then(notificationRepository).should()
                .findById(notificationId);

        then(notificationRepository).should()
                .delete(notification);

        then(notificationRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 존재하지 않는 알림이면 NotificationNotFoundException 발생")
    void markAsReadAndDeleteNotificationFailWithNotFound() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                notificationService.markAsReadAndDeleteNotification(notificationId, currentUserId)
        )
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("알림을 찾을 수 없습니다.");

        then(notificationRepository).should().findById(notificationId);
        then(notificationRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 본인의 알림이 아니면 NotificationReadDeniedException 발생")
    void markAsReadAndDeleteNotificationFail_NoPermission() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID(); // 다른 사람 ID

        User receiver = Mockito.mock(User.class);
        Notification notification = Mockito.mock(Notification.class);

        given(notification.getReceiver()).willReturn(receiver);
        given(receiver.getId()).willReturn(otherUserId);

        given(notificationRepository.findById(notificationId))
                .willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() ->
                notificationService.markAsReadAndDeleteNotification(notificationId, currentUserId)
        )
                .isInstanceOf(NotificationReadDeniedException.class)
                .hasMessage("본인의 알림만 읽을 수 있습니다.");

        then(notificationRepository).should().findById(notificationId);
        then(notificationRepository).shouldHaveNoMoreInteractions();
    }
}
