package com.codeit.playlist.domain.playlist.repository.custom;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PlaylistRepositoryCustom {

    Slice<Playlist> searchPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorId") UUID cursorId,
            @Param("asc") boolean asc,
            String sortBy,
            Pageable pageable
    );

    long countPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual
    );
}
