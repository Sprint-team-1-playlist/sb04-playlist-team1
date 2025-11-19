package com.codeit.playlist.domain.message.service.basic;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.service.MessageService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  private final MessageMapper messageMapper;
  private final ConversationRepository conversationRepository;

  @Override
  public DirectMessageDto save(UUID conversationId, DirectMessageSendRequest sendRequest) {
    log.debug("[Message] 메시지 저장 시작: {}", conversationId);

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(ConversationNotFoundException::new);

    UUID currentUserId = getCurrentUserId();

    if (!conversation.getUser1().getId().equals(currentUserId)
        && !conversation.getUser2().getId().equals(currentUserId)) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    UUID user1Id = conversation.getUser1().getId();
    UUID user2Id = conversation.getUser2().getId();

    if (!user1Id.equals(currentUserId) && !user2Id.equals(currentUserId)) {
      throw ConversationNotFoundException.withId(conversationId);
    }

    User sender = user1Id.equals(currentUserId)
        ? conversation.getUser1()
        : conversation.getUser2();

    User receiver = user1Id.equals(currentUserId)
        ? conversation.getUser2()
        : conversation.getUser1();

    Message savedMessage = messageRepository.save(new Message(conversation, sender, receiver, sendRequest.content()));

    log.info("[Message] 메시지 저장 완료: {}", savedMessage.getId());

    DirectMessageDto messageDto = messageMapper.toDto(savedMessage);
    return messageDto;
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    return userDetails.getUserDto().id();
  }
}
