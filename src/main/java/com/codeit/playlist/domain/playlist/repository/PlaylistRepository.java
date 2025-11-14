package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    @Query("""
        select p
        from Playlist p
        where (:keywordLike is null or p.title like concat('%', :keywordLike, '%'))
          and (:ownerIdEqual is null or p.owner.id = :ownerIdEqual)
          and (:subscriberIdEqual is null
               or exists (
                    select s.id
                    from Subscribe s
                    where s.playlist = p and s.subscriber.id = :subscriberIdEqual
               )
          )
          and (:hasCursor = false
               or (:asc = true and p.id > :cursorId)
               or (:asc = false and p.id < :cursorId))
        """)
    Slice<Playlist> searchPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorId") UUID cursorId,
            @Param("asc") boolean asc,
            Pageable pageable
    );

    @Query("""
        select count(p)
        from Playlist p
        where (:keywordLike is null or p.title like concat('%', :keywordLike, '%'))
          and (:ownerIdEqual is null or p.owner.id = :ownerIdEqual)
          and (:subscriberIdEqual is null
               or exists (
                    select s.id
                    from Subscribe s
                    where s.playlist = p and s.subscriber.id = :subscriberIdEqual
               )
          )
        """)
    long countPlaylists(
            @Param("keywordLike") String keywordLike,
            @Param("ownerIdEqual") UUID ownerIdEqual,
            @Param("subscriberIdEqual") UUID subscriberIdEqual
    );
}
