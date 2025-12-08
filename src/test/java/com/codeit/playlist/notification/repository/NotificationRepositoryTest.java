package com.codeit.playlist.notification.repository;

import com.codeit.playlist.domain.notification.entity.Level;
import com.codeit.playlist.domain.notification.entity.Notification;
import com.codeit.playlist.domain.notification.repository.NotificationRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
public class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("알림 커서 조회 성공 - 첫 페이지와 다음 페이지, 총 개수 확인")
    void findByReceiverIdWithCursorPagingSuccess() {
        // given
        User receiver = createTestUser("receiver@test.com");
        em.persist(receiver);

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            Notification n = createTestNotification(
                    receiver,
                    "알림-" + i,
                    base.minus(i, ChronoUnit.MINUTES)
            );
            em.persist(n);
        }

        // 다른 사용자 알림도 섞어서 저장 (조회 시 필터링되는지 확인 용도)
        User otherUser = createTestUser("other@test.com");
        em.persist(otherUser);

        Notification otherNotification = createTestNotification(
                otherUser,
                "다른 유저 알림",
                base.minus(10, ChronoUnit.MINUTES)
        );
        em.persist(otherNotification);

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(
                0,
                3,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        // when
        Slice<Notification> firstSlice =
                notificationRepository.findByReceiverIdWithCursorPaging(receiver.getId(), null, null, pageable);

        Notification lastOfFirst = firstSlice.getContent().get(2);
        Instant cursorCreatedAt = lastOfFirst.getCreatedAt();
        UUID cursorId = firstSlice.getContent().get(2).getId();

        Slice<Notification> secondSlice =
                notificationRepository.findByReceiverIdWithCursorPaging(receiver.getId(), cursorCreatedAt, cursorId, pageable);

        // then
        assertThat(firstSlice.getContent()).hasSize(3);
        assertThat(firstSlice.hasNext()).isTrue();
        assertThat(firstSlice.getContent())
                .extracting(Notification::getReceiver)
                .allSatisfy(user -> assertThat(user.getId()).isEqualTo(receiver.getId()));

        assertThat(secondSlice.getContent()).hasSize(2);
        assertThat(secondSlice.hasNext()).isFalse();
        assertThat(secondSlice.getContent())
                .extracting(Notification::getReceiver)
                .allSatisfy(user -> assertThat(user.getId()).isEqualTo(receiver.getId()));

        // then - countByReceiver_Id 검증
        long count = notificationRepository.countByReceiver_Id(receiver.getId());
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("알림 커서 조회 실패 - 커서가 범위를 벗어나면 빈 결과와 hasNext=false 반환")
    void findByReceiverIdWithCursorPagingFailWithOutOfRangeCursor() {
        // given
        User receiver = createTestUser("receiver@test.com");
        em.persist(receiver);

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 3; i++) {
            Notification n = createTestNotification(
                    receiver,
                    "알림-" + i,
                    base.minus(i, ChronoUnit.MINUTES)
            );
            em.persist(n);
        }

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(
                0,
                3,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Instant outOfRangeCursorCreatedAt = base.minus(1,ChronoUnit.DAYS);

        // when
        Slice<Notification> slice =
                notificationRepository.findByReceiverIdWithCursorPaging(
                        receiver.getId(),
                        outOfRangeCursorCreatedAt,
                        null,
                        pageable
                );

        // then
        assertThat(slice.getContent()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("알림 커서 조회 실패 - 해당 수신자의 알림이 하나도 없으면 빈 결과와 0 카운트 반환")
    void findByReceiverIdWithCursorPagingFailWhenNoNotification() {
        // given
        User receiver = createTestUser("no-notification@test.com");
        em.persist(receiver);
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // when
        Slice<Notification> slice =
                notificationRepository.findByReceiverIdWithCursorPaging(receiver.getId(), null, null, pageable);
        long count = notificationRepository.countByReceiver_Id(receiver.getId());

        // then
        assertThat(slice.getContent()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
        assertThat(count).isZero();
    }

    private User createTestUser(String email) {
        User user = new User(email, "password", "test-user", null, Role.USER);

        Instant now = Instant.now();
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);

        return user;
    }

    private Notification createTestNotification(User receiver, String title, Instant createdAt) {
        Notification notification = new Notification(
                receiver,
                title,
                "테스트 내용",
                Level.INFO
        );

        ReflectionTestUtils.setField(notification, "createdAt", createdAt);

        return notification;
    }
}
