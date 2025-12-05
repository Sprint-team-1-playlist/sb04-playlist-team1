package com.codeit.playlist.domain.message.service.basic;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.message.dto.data.MessageSortBy;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.message.event.message.DirectMessageSentEvent;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.exception.InvalidMessageReadOperationException;
import com.codeit.playlist.domain.message.exception.MessageNotFoundException;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.service.MessageService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.entity.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  private final MessageMapper messageMapper;
  private final ConversationRepository conversationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public DirectMessageDto save(UUID conversationId, DirectMessageSendRequest sendRequest) {
    log.debug("[Message] 메시지 저장 시작: {}", conversationId);

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> ConversationNotFoundException.withConversationId(conversationId));

    UUID currentUserId = getCurrentUserId();

    if (!conversation.isParticipant(currentUserId)) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    UUID user1Id = conversation.getUser1().getId();

    User sender = user1Id.equals(currentUserId)
        ? conversation.getUser1()
        : conversation.getUser2();

    User receiver = user1Id.equals(currentUserId)
        ? conversation.getUser2()
        : conversation.getUser1();

    Message savedMessage = messageRepository.save(new Message(conversation, sender, receiver, sendRequest.content()));

    conversation.markAsUnread();
    log.info("[Message] 메시지 저장 완료: {}", savedMessage.getId());

    DirectMessageDto messageDto = messageMapper.toDto(savedMessage);

    eventPublisher.publishEvent(new DirectMessageSentEvent(conversationId, messageDto));

    return messageDto;
  }

  @Transactional(readOnly = true)
  @Override
  public CursorResponseDirectMessageDto findAll(UUID conversationId, String cursor,
      UUID idAfter, int limit, SortDirection sortDirection, MessageSortBy sortBy) {

    log.debug("[Message] DM 목록 조회 시작: {}", conversationId);

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> ConversationNotFoundException.withConversationId(conversationId));

    UUID currentUserId = getCurrentUserId();

    if (!conversation.isParticipant(currentUserId)) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    LocalDateTime cursorTime = parseCursor(cursor);

    Pageable pageable = PageRequest.of(0, limit + 1);
    List<Message> messages = messageRepository.findMessagesByConversationWithCursor(
        conversationId, cursorTime, idAfter, pageable);

    String nextCursor = null;
    UUID nextIdAfter = null;
    List<Message> pageMessages = messages.size() > limit
        ? messages.subList(0, limit)
        : messages;

    long totalCount = messageRepository.countByConversationId(conversationId);

    boolean hasNext = totalCount > pageMessages.size();

    if (!pageMessages.isEmpty()) {
      Message last = pageMessages.get(pageMessages.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    List<DirectMessageDto> content = pageMessages.stream()
        .map(messageMapper::toDto)
        .collect(Collectors.toList());

    CursorResponseDirectMessageDto cursorMessageDto = new CursorResponseDirectMessageDto(content,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy,
        sortDirection
    );

    log.info("[Message] DM 목록 조회 완료: conversationId={}, count={}", conversationId, content.size());

    return cursorMessageDto;
  }

  @Override
  public void markMessageAsRead(UUID conversationId, UUID directMessageId) {
    log.debug("[Message] DM 읽음 처리 시작: {}, {}", conversationId, directMessageId);

    Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> ConversationNotFoundException.withConversationId(conversationId));

    UUID currentUserId = getCurrentUserId();
    if (!conversation.isParticipant(currentUserId)) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    Message message = messageRepository.findById(directMessageId)
        .orElseThrow(() -> MessageNotFoundException.withId(directMessageId));

    validateReadMessage(message, conversation);

    conversation.markAsRead();

    log.info("[Message] DM 읽음 처리 완료");
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    return userDetails.getUserDto().id();
  }

  private LocalDateTime parseCursor(String cursor) {
    if (cursor == null) return null;
    try {
      return LocalDateTime.parse(cursor);
    } catch (DateTimeParseException e) {
      throw InvalidCursorException.withCursor(cursor);
    }
  }

  private void validateReadMessage(Message message, Conversation conversation) {
    Optional<Message> latest = messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation);
    if (latest.isEmpty() || !latest.get().getId().equals(message.getId())) {
      throw InvalidMessageReadOperationException.withId(message.getId());
    }
  }
}
