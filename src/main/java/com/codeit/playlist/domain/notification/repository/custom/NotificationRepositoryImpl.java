package com.codeit.playlist.domain.notification.repository.custom;

import com.codeit.playlist.domain.notification.entity.Notification;
import com.codeit.playlist.domain.notification.entity.QNotification;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<Notification> findByReceiverIdWithCursorPaging(UUID receiverId, Instant cursorCreatedAt,
                                                                UUID cursorId, Pageable pageable) {

        QNotification notification = QNotification.notification;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(notification.receiver.id.eq(receiverId));

        if (cursorCreatedAt != null) {

            Sort.Order createdAtOrder = pageable.getSort().stream()
                    .filter(o -> o.getProperty().equals("createdAt"))
                    .findFirst()
                    .orElse(new Sort.Order(Sort.Direction.DESC, "createdAt"));

            boolean ascending = createdAtOrder.getDirection().isAscending();

            if (ascending) {
                // createdAt 오름차순
                if (cursorId != null) {
                    builder.and(
                            notification.createdAt.gt(cursorCreatedAt)
                                    .or(notification.createdAt.eq(cursorCreatedAt)
                                            .and(notification.id.gt(cursorId)))
                    );
                } else {
                    builder.and(notification.createdAt.gt(cursorCreatedAt));
                }
            } else {
                // createdAt 내림차순
                if (cursorId != null) {
                    builder.and(
                            notification.createdAt.lt(cursorCreatedAt)
                                    .or(notification.createdAt.eq(cursorCreatedAt)
                                            .and(notification.id.lt(cursorId)))
                    );
                } else {
                    builder.and(notification.createdAt.lt(cursorCreatedAt));
                }
            }
        }

        // 정렬 순서 구성
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(notification, pageable);

        int pageSize = pageable.getPageSize();

        // 3. limit+1 조회 후 Slice로 포장
        List<Notification> content = jpaQueryFactory
                .selectFrom(notification)
                .leftJoin(notification.receiver).fetchJoin()
                .where(builder)
                .orderBy(orders.toArray(OrderSpecifier[]::new))
                .limit(pageSize + 1L)
                .fetch();

        boolean hasNext = content.size() > pageSize;
        if (hasNext) {
            content.remove(pageSize);
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 정렬 로직 분리
    private List<OrderSpecifier<?>> buildOrderSpecifiers(
            QNotification notification, Pageable pageable) {

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            boolean asc = order.getDirection().isAscending();
            switch (order.getProperty()) {
                case "createdAt" -> orders.add(
                        asc ? notification.createdAt.asc() : notification.createdAt.desc()
                );
                case "id" -> orders.add(
                        asc ? notification.id.asc() : notification.id.desc()
                );
            }
        });

        // 방어적 코드: 정렬 정보가 없을 때 기본값
        if (orders.isEmpty()) {
            orders.add(notification.createdAt.desc());
            orders.add(notification.id.desc());
        }

        return orders;
    }
}
