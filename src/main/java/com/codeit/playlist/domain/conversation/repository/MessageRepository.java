package com.codeit.playlist.domain.conversation.repository;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.entity.Message;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

  Optional<Message> findFirstByConversationOrderByCreatedAtDesc(Conversation conversation);
}
