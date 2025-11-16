package com.codeit.playlist.domain.playlist.repository.custom;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.QPlaylist;
import com.codeit.playlist.domain.playlist.entity.QSubscribe;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements PlaylistRepositoryCustom {

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
            Pageable pageable
    ) {
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

        // 커서 조건 (keyset 기준 id)
        if (hasCursor && cursorId != null) {
            if (asc) {
                builder.and(playlist.id.gt(cursorId));
            } else {
                builder.and(playlist.id.lt(cursorId));
            }
        }

        // 정렬 조건
        OrderSpecifier<?> orderById =
                asc ? playlist.id.asc() : playlist.id.desc();

        List<Playlist> results = query
                .selectFrom(playlist)
                .leftJoin(playlist.owner).fetchJoin()
                .where(builder)
                .orderBy(orderById)
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

        Long result = query
                .select(playlist.count())
                .from(playlist)
                .where(builder)
                .fetchOne();

        return result != null ? result : 0L;
    }
}
