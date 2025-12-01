package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.dto.request.ContentCursorRequest;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.QContent;
import com.codeit.playlist.domain.content.entity.Type;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public List<Content> searchContents(ContentCursorRequest request, boolean ascending) {
        QContent qContent = QContent.content;
        BooleanBuilder builder = new BooleanBuilder(); // BooleanBuilder
        log.info("searchContents: ascending={}, sortBy={}, cursor={}, idAfter={}",
                ascending, request.sortBy(), request.cursor(), request.idAfter());
        // 검색 조건
        if (request.typeEqual() != null) {
            builder.and(qContent.type.eq(Type.valueOf(request.typeEqual().toUpperCase())));
        }

        if (request.keywordLike() != null) {
            builder.and(qContent.title.containsIgnoreCase(request.keywordLike()));
        }

        // 정렬 기준, 정렬 방향
        String sortBy = request.sortBy() == null ? "createdAt" : request.sortBy();
        Order order = ascending ? Order.ASC : Order.DESC;

        // 커서를 여기에서 씀
        String cursor = request.cursor();
        String after = request.idAfter();

        if (cursor != null && after != null) {
            UUID cursorId = UUID.fromString(after);

            switch (sortBy) {
                case "createdAt":
                    LocalDateTime cursorDt = LocalDateTime.parse(cursor);

                    if (ascending) {
                        builder.and(qContent.createdAt.gt(cursorDt)
                                .or(qContent.createdAt.eq(cursorDt).and(qContent.id.gt(cursorId))));
                    } else {
                        builder.and(qContent.createdAt.lt(cursorDt)
                                .or(qContent.createdAt.eq(cursorDt).and(qContent.id.lt(cursorId))));
                    }
                    break;

                case "watcherCount":
                    Integer cursorWatch = Integer.valueOf(cursor);

                    if (ascending) {
                        builder.and(qContent.watcherCount.gt(cursorWatch)
                                .or(qContent.watcherCount.eq(cursorWatch).and(qContent.id.gt(cursorId))));
                    } else {
                        builder.and(qContent.watcherCount.lt(cursorWatch)
                                .or(qContent.watcherCount.eq(cursorWatch).and(qContent.id.lt(cursorId))));
                    }
                    break;

                case "rate":
                    Double cursorRate = Double.valueOf(cursor);

                    if (ascending) {
                        builder.and(qContent.averageRating.gt(cursorRate)
                                .or(qContent.averageRating.eq(cursorRate).and(qContent.id.gt(cursorId))));
                    } else {
                        builder.and(qContent.averageRating.lt(cursorRate)
                                .or(qContent.averageRating.eq(cursorRate).and(qContent.id.lt(cursorId))));
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Invalid sortBy=" + sortBy);
            }
        }

        // 정렬 + limit+1
        int limit = request.limit() <= 0 ? 30 : request.limit();

        return query.selectFrom(qContent)
                .where(builder)
                .orderBy(getOrderSpecifier(sortBy, order),
                        new OrderSpecifier<>(order, qContent.id))
                .limit(limit + 1)
                .fetch();
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortBy, Order order) {
        QContent c = QContent.content;

        switch (sortBy) {
            case "createdAt":
                return new OrderSpecifier<>(order, c.createdAt);

            case "watcherCount":
                return new OrderSpecifier<>(order, c.watcherCount);

            case "rate":
                return new OrderSpecifier<>(order, c.averageRating);

            default:
                throw new IllegalArgumentException("Invalid sortBy=" + sortBy);
        }
    }

    @Override
    public long countContents(ContentCursorRequest request) {
        QContent c = QContent.content;
        BooleanBuilder builder = new BooleanBuilder();

        // 검색 조건만 적용
        if (request.typeEqual() != null) {
            builder.and(c.type.eq(Type.valueOf(request.typeEqual().toUpperCase())));
        }

        if (request.keywordLike() != null) {
            builder.and(c.title.containsIgnoreCase(request.keywordLike()));
        }

        long result = query.select(c.count()).from(c).where(builder).fetchOne();

        return result;
    }
}
