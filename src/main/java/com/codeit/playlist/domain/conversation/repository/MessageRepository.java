package com.codeit.playlist.domain.conversation.repository;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.entity.Message;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

  Optional<Message> findFirstByConversationOrderByCreatedAtDesc(Conversation conversation);

  @Query("SELECT m FROM Message m WHERE m.conversation IN :conversations " +
      "AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.conversation = m.conversation)")
  List<Message> findLatestMessagesByConversations(@Param("conversations") List<Conversation> conversations);
}
