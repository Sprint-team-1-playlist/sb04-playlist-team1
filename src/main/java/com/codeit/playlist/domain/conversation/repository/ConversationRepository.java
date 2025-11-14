package com.codeit.playlist.domain.conversation.repository;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  @Query("SELECT c FROM Conversation c " +
      "WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) " +
      "   OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
  Optional<Conversation> findByUserIds(@Param("userId1") UUID userId1,
      @Param("userId2") UUID userId2);
}
