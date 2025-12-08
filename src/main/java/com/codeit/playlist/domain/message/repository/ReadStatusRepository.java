package com.codeit.playlist.domain.message.repository;

import com.codeit.playlist.domain.message.entity.ReadStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, UUID> {

  boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

  @Query("""
        SELECT COUNT(m) > 0
        FROM Message m
        LEFT JOIN ReadStatus r
            ON r.message.id = m.id AND r.user.id = :userId
        WHERE m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND r.id IS NULL
    """)
  boolean hasUnread(UUID conversationId, UUID userId);
}
