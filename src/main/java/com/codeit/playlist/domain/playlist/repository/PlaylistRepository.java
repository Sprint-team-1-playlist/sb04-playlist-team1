package com.codeit.playlist.domain.playlist.repository;

import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.repository.custom.PlaylistRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

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
    List<Playlist> findAllDeletedBefore(@Param("threshold") LocalDateTime threshold);

    //플레이리스트 단건 조회
    @EntityGraph(attributePaths = {
            "owner",
            "playlistContents",                    // 플레이리스트-콘텐츠 연결 엔티티
            "playlistContents.content",            // 실제 콘텐츠
            "playlistContents.content.tags"        // 태그
    })
    Optional<Playlist> findWithDetailsById(UUID id);
}
