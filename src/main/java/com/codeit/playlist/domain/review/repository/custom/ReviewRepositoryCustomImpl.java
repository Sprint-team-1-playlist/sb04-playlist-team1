package com.codeit.playlist.domain.review.repository.custom;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.review.entity.QReview;
import com.codeit.playlist.domain.review.entity.Review;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    private static final QReview review = QReview.review;

    @Override
    public Slice<Review> findReviews(
           UUID contentId,
           String cursor,
           UUID idAfter,
           int limit,
           SortDirection sortDirection,
           String sortBy
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        //콘텐츠 필터
        if (contentId != null) {
            builder.and(review.content.id.eq(contentId));
        }

        //커서 해석 (cursor 우선, 없으면 idAfter)
        UUID cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorId = UUID.fromString(cursor);
            } catch (IllegalArgumentException e) {
                // 잘못된 형식이면 무시
                cursorId = null;
            }
        }
        if (cursorId == null && idAfter != null) {
            cursorId = idAfter;
        }

        //정렬 방향
        boolean asc = (sortDirection == SortDirection.ASCENDING);
        Order mainOrder = asc ? Order.ASC : Order.DESC;
        Order idOrder = asc ? Order.ASC : Order.DESC;

        //커서 조건 (createdAt / rating + id 안정 정렬)
        BooleanExpression cursorCondition = createCursorCondition(cursorId, asc, sortBy);
        if (cursorCondition != null) {
            builder.and(cursorCondition);
        }

        //정렬 조건
        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(mainOrder, idOrder, sortBy);

        // limit + 1 조회
        List<Review> results = jpaQueryFactory
                .selectFrom(review)
                .where(builder)
                .orderBy(orderSpecifiers)
                .limit((long) limit + 1L)
                .fetch();

        boolean hasNext = results.size() > limit;
        if (hasNext) {
            results.remove(limit);
        }

        return new SliceImpl<>(results, PageRequest.of(0, limit), hasNext);
    }

    //커서 기준 where 조건 생성
    private BooleanExpression createCursorCondition(UUID cursorId, boolean asc, String sortBy) {
        if (cursorId == null) {
            return null;
        }

        // 커서에 해당하는 리뷰 조회
        Review cursorReview = jpaQueryFactory
                .selectFrom(review)
                .where(review.id.eq(cursorId))
                .fetchOne();

        if (cursorReview == null) {
            return null;
        }

        // createdAt 기준
        if ("createdAt".equals(sortBy)) {
            Instant cursorCreatedAt = cursorReview.getCreatedAt();

            if (asc) {
                return review.createdAt.gt(cursorCreatedAt)
                        .or(review.createdAt.eq(cursorCreatedAt)
                                .and(review.id.gt(cursorId)));
            } else {
                return review.createdAt.lt(cursorCreatedAt)
                        .or(review.createdAt.eq(cursorCreatedAt)
                                .and(review.id.lt(cursorId)));
            }
        }

        // rating 기준
        if ("rating".equals(sortBy)) {
            int cursorRating = cursorReview.getRating();

            if (asc) {
                return review.rating.gt(cursorRating)
                        .or(review.rating.eq(cursorRating)
                                .and(review.id.gt(cursorId)));
            } else {
                return review.rating.lt(cursorRating)
                        .or(review.rating.eq(cursorRating)
                                .and(review.id.lt(cursorId)));
            }
        }

        // 이 외 sortBy 값은 처리하지 않음
        return null;
    }

    //정렬 조건 생성
    private OrderSpecifier<?>[] createOrderSpecifiers(Order mainOrder, Order idOrder, String sortBy) {
        OrderSpecifier<?> mainSort;

        if ("rating".equals(sortBy)) {
            mainSort = new OrderSpecifier<>(mainOrder, review.rating);
        } else { // 기본값: createdAt
            mainSort = new OrderSpecifier<>(mainOrder, review.createdAt);
        }

        OrderSpecifier<?> idSort = new OrderSpecifier<>(idOrder, review.id);

        return new OrderSpecifier<?>[]{mainSort, idSort};
    }
}
