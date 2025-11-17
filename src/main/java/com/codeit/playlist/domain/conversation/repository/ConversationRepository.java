package com.codeit.playlist.domain.conversation.repository;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  @Query("SELECT c FROM Conversation c " +
      "WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) " +
      "   OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
  Optional<Conversation> findByUserIds(@Param("userId1") UUID userId1,
      @Param("userId2") UUID userId2);

  @Query("""
    SELECT c
    FROM Conversation c
    JOIN c.user2 u
    WHERE (:keyword IS NULL OR u.name LIKE %:keyword%)
      AND (
            :cursor IS NULL
            OR (c.createdAt < :cursor)
            OR (c.createdAt = :cursor AND c.id < :idAfter)
          )
    ORDER BY c.createdAt DESC, c.id DESC
    """)
  List<Conversation> findPageDesc(
      @Param("keyword") String keyword,
      @Param("cursor") String cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
    SELECT c
    FROM Conversation c
    JOIN c.user2 u
    WHERE (:keyword IS NULL OR u.name LIKE %:keyword%)
      AND (
            :cursor IS NULL
            OR (c.createdAt > :cursor)
            OR (c.createdAt = :cursor AND c.id > :idAfter)
          )
    ORDER BY c.createdAt ASC, c.id ASC
    """)
  List<Conversation> findPageAsc(
      @Param("keyword") String keyword,
      @Param("cursor") String cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
        SELECT COUNT(c)
        FROM Conversation c
        JOIN c.user2 u
        WHERE (:keyword IS NULL OR u.name LIKE %:keyword%)
        """)
  long countAll(@Param("keyword") String keyword);
}
