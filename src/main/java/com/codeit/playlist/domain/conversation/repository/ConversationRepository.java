package com.codeit.playlist.domain.conversation.repository;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  @Query("SELECT c FROM Conversation c " +
      "WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) " +
      "   OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
  Optional<Conversation> findByUserIds(@Param("userId1") UUID userId1,
      @Param("userId2") UUID userId2);

  @Query("""
    SELECT c
    FROM Conversation c
    LEFT JOIN FETCH c.user1 u1
    LEFT JOIN FETCH c.user2 u2
    WHERE (c.user1.id = :currentUserId OR c.user2.id = :currentUserId)
      AND (:#{#keyword == null} = true OR u1.name LIKE CONCAT('%', :keyword, '%') OR u2.name LIKE CONCAT('%', :keyword, '%'))
      AND (
            :#{#cursor == null} = true
            OR (c.createdAt < :cursor)
            OR (c.createdAt = :cursor AND c.id < :idAfter)
          )
    ORDER BY c.createdAt DESC, c.id DESC
    """)
  List<Conversation> findPageDesc(
      @Param("currentUserId") UUID currentUserId,
      @Param("keyword") String keyword,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
    SELECT c
    FROM Conversation c
    LEFT JOIN FETCH c.user1 u1
    LEFT JOIN FETCH c.user2 u2
    WHERE (c.user1.id = :currentUserId OR c.user2.id = :currentUserId)
      AND (:#{#keyword == null} = true OR u1.name LIKE CONCAT('%', :keyword, '%') OR u2.name LIKE CONCAT('%', :keyword, '%'))
      AND (
            :#{#cursor == null} = true
            OR (c.createdAt > :cursor)
            OR (c.createdAt = :cursor AND c.id > :idAfter)
          )
    ORDER BY c.createdAt ASC, c.id ASC
    """)
  List<Conversation> findPageAsc(
      @Param("currentUserId") UUID currentUserId,
      @Param("keyword") String keyword,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
        SELECT COUNT(c)
        FROM Conversation c
        WHERE (c.user1.id = :currentUserId OR c.user2.id = :currentUserId)
          AND (:#{#keyword == null} = true OR c.user1.name LIKE CONCAT('%', :keyword, '%') OR c.user2.name LIKE CONCAT('%', :keyword, '%'))
        """)
  long countAll(@Param("currentUserId") UUID currentUserId, @Param("keyword") String keyword);
}
