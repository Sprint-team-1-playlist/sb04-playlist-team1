package com.codeit.playlist.domain.message.service.basic;

import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.exception.InvalidCursorException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.service.MessageService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.entity.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional(readOnly = true)
  @Override
  public CursorResponseDirectMessageDto findAll(UUID conversationId, String cursor,
      UUID idAfter, int limit, String sortDirection, String sortBy) {

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> ConversationNotFoundException.withId(conversationId));

    UUID currentUserId = getCurrentUserId();

    if (!conversation.getUser1().getId().equals(currentUserId)
        && !conversation.getUser2().getId().equals(currentUserId)) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    LocalDateTime cursorTime = null;
    if (cursor != null) {
      try {
        cursorTime = LocalDateTime.parse(cursor);
      } catch (DateTimeParseException e) {
        throw InvalidCursorException.withCursor(cursor);
      }

    }
    Pageable pageable = PageRequest.of(0, limit + 1);
    List<Message> messages = messageRepository.findMessagesByConversationWithCursor(
        conversationId, cursorTime, idAfter, pageable);

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = messages.size() > limit;

    List<Message> pageMessages = hasNext
        ? messages.subList(0, limit)
        : messages;

    if (!pageMessages.isEmpty()) {
      Message last = pageMessages.get(pageMessages.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    List<DirectMessageDto> content = pageMessages.stream()
        .map(messageMapper::toDto)
        .collect(Collectors.toList());

    long totalCount = messageRepository.countByConversationId(conversationId);

    return new CursorResponseDirectMessageDto(content,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy,
        sortDirection
    );
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    return userDetails.getUserDto().id();
  }
}
