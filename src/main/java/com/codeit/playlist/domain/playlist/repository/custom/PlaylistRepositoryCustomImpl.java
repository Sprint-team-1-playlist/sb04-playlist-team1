package com.codeit.playlist.domain.playlist.repository.custom;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.QPlaylist;
import com.codeit.playlist.domain.playlist.entity.QSubscribe;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class PlaylistRepositoryCustomImpl implements PlaylistRepositoryCustom {

    private final JPAQueryFactory query;

    private final QPlaylist playlist = QPlaylist.playlist;
    private final QSubscribe subscribe = QSubscribe.subscribe;

    @Override
    public Slice<Playlist> searchPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorId") UUID cursorId,
            @Param("asc") boolean asc,
            String sortBy,
            Pageable pageable
    ) {
        BooleanBuilder builder = buildFilterConditions(keywordLike, ownerIdEqual, subscriberIdEqual);

        BooleanExpression cursorCondition = createCursorCondition(hasCursor, cursorId, asc, sortBy);
        if (cursorCondition != null) {
            builder.and(cursorCondition);
        }

        // 정렬 조건
        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortBy, asc);

        List<Playlist> results = query
                .selectFrom(playlist)
                .leftJoin(playlist.owner).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifiers)
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();

        if (hasNext) {
            results.remove(results.size() - 1);
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }

    @Override
    public long countPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual
    ) {

        BooleanBuilder builder = buildFilterConditions(keywordLike, ownerIdEqual, subscriberIdEqual);

        Long result = query
                .select(playlist.count())
                .from(playlist)
                .where(builder)
                .fetchOne();

        return result != null ? result : 0L;
    }

    private BooleanBuilder buildFilterConditions(String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual) {
        // 동적 조건
        BooleanBuilder builder = new BooleanBuilder();

        if (keywordLike != null) {
            builder.and(playlist.title.containsIgnoreCase(keywordLike));
        }

        if (ownerIdEqual != null) {
            builder.and(playlist.owner.id.eq(ownerIdEqual));
        }

        if (subscriberIdEqual != null) {
            builder.and(
                    JPAExpressions
                            .selectOne()
                            .from(subscribe)
                            .where(
                                    subscribe.playlist.eq(playlist),
                                    subscribe.subscriber.id.eq(subscriberIdEqual)
                            )
                            .exists()
            );
        }

        return builder;
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(String sortBy, boolean asc) {

        OrderSpecifier<?> primary;
        OrderSpecifier<?> secondary;

        if ("subscriberCount".equals(sortBy)) {
            primary = asc ? playlist.subscriberCount.asc() : playlist.subscriberCount.desc();
        } else {
            // default = updatedAt
            primary = asc ? playlist.updatedAt.asc() : playlist.updatedAt.desc();
        }

        // 항상 보조 정렬은 id
        secondary = asc ? playlist.id.asc() : playlist.id.desc();

        return new OrderSpecifier[]{ primary, secondary };
    }


    private BooleanExpression createCursorCondition(
            boolean hasCursor,
            UUID cursorId,
            boolean asc,
            String sortBy
    ) {
        if (!hasCursor || cursorId == null) {
            return null;
        }

        // 커서를 DB에서 가져와야 primary sort 값을 비교할 수 있음
        Playlist cursorPlaylist = query
                .selectFrom(playlist)
                .where(playlist.id.eq(cursorId))
                .fetchOne();

        if (cursorPlaylist == null) {
            return null;
        }

        BooleanExpression primaryCompare;
        BooleanExpression tieBreakerCompare;

        if ("subscriberCount".equals(sortBy)) {
            Long cursorCount = cursorPlaylist.getSubscriberCount();

            if (asc) {
                primaryCompare = playlist.subscriberCount.gt(cursorCount);
                tieBreakerCompare = playlist.subscriberCount.eq(cursorCount)
                        .and(playlist.id.gt(cursorId));
            } else {
                primaryCompare = playlist.subscriberCount.lt(cursorCount);
                tieBreakerCompare = playlist.subscriberCount.eq(cursorCount)
                        .and(playlist.id.lt(cursorId));
            }

        } else { // updatedAt
            LocalDateTime cursorUpdatedAt = cursorPlaylist.getUpdatedAt();

            if (asc) {
                primaryCompare = playlist.updatedAt.gt(cursorUpdatedAt);
                tieBreakerCompare = playlist.updatedAt.eq(cursorUpdatedAt)
                        .and(playlist.id.gt(cursorId));
            } else {
                primaryCompare = playlist.updatedAt.lt(cursorUpdatedAt);
                tieBreakerCompare = playlist.updatedAt.eq(cursorUpdatedAt)
                        .and(playlist.id.lt(cursorId));
            }
        }

        return primaryCompare.or(tieBreakerCompare);
    }

}
