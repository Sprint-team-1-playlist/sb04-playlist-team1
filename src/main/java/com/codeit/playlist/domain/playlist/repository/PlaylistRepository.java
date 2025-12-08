package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.repository.custom.PlaylistRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

    //삭제되지 않은 플레이리스트 조회
    Optional<Playlist> findByIdAndDeletedAtIsNull(UUID id);

    // Soft delete
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Playlist p 
           set p.deletedAt = CURRENT_TIMESTAMP 
         where p.id = :playlistId 
           and p.deletedAt is null
    """)
    int softDeleteById(@Param("playlistId") UUID playlistId);

    // 7일 지난 soft delete 대상 조회
    @Query("""
        select p 
          from Playlist p 
         where p.deletedAt is not null
                and p.deletedAt <= :threshold
    """)
    List<Playlist> findAllDeletedBefore(@Param("threshold") Instant threshold);

    //플레이리스트 단건 조회
    @EntityGraph(attributePaths = {
            "owner",
            "playlistContents",                    // 플레이리스트-콘텐츠 연결 엔티티
            "playlistContents.content",            // 실제 콘텐츠
//            "playlistContents.content.tags"        // 태그
    })
    @Query("select p from Playlist p " +
            "where p.id = :id " +
            "and p.deletedAt is null")
    Optional<Playlist> findWithDetailsById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Playlist p 
       set p.subscriberCount = p.subscriberCount + 1 
     where p.id = :playlistId
     and p.deletedAt is null
""")
    int increaseSubscriberCount(@Param("playlistId") UUID playlistId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Playlist p 
       set p.subscriberCount = p.subscriberCount - 1 
     where p.id = :playlistId
       and p.subscriberCount > 0
       and p.deletedAt is null 
""")
    int decreaseSubscriberCount(@Param("playlistId") UUID playlistId);

}
