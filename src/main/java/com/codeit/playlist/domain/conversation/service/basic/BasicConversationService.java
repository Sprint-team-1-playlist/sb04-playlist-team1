package com.codeit.playlist.domain.conversation.service.basic;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationAlreadyExistsException;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.exception.InvalidCursorException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.exception.SelfChatNotAllowedException;
import com.codeit.playlist.domain.conversation.mapper.ConversationMapper;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.conversation.service.ConversationService;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@RequiredArgsConstructor
@Transactional
@Service
public class BasicConversationService implements ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationMapper conversationMapper;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final MessageRepository messageRepository;
  private final MessageMapper messageMapper;

  @Override
  public ConversationDto create(ConversationCreateRequest request) {
    log.debug("[Conversation] 대화 생성 시작 {}", request);

    UUID currentUserId = getCurrentUserId();
    if (currentUserId.equals(request.withUserId())){
      throw SelfChatNotAllowedException.withId(currentUserId);
    }

    Optional<Conversation> existingConversation = conversationRepository
        .findByUserIds(currentUserId, request.withUserId());
    if (existingConversation.isPresent()) {
      throw ConversationAlreadyExistsException.withId(existingConversation.get().getId());
    }

    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> UserNotFoundException.withId(currentUserId));
    User user = userRepository.findById(request.withUserId())
        .orElseThrow(() -> UserNotFoundException.withId(request.withUserId()));

    Conversation conversation = new Conversation(currentUser,user);
    conversationRepository.save(conversation);

    ConversationDto conversationDto = toConversationDto(conversation);

    log.info("[Conversation] 대화 생성 완료 {}", conversationDto.id());
    return conversationDto;
  }

  @Transactional(readOnly = true)
  @Override
  public CursorResponseConversationDto findAll(
      String keywordLike,
      String cursor,
      UUID idAfter,
      int limit,
      String sortDirection,
      String sortBy
  ) {
    log.debug("[Conversation] 대화 조회 시작");

    boolean isAsc = sortDirection.equalsIgnoreCase("ASCENDING");

    Pageable pageable = PageRequest.of(0, limit + 1);

    UUID currentUserId = getCurrentUserId();

    LocalDateTime cursorTime = null;
    if (cursor != null) {
      try {
        cursorTime = LocalDateTime.parse(cursor);
      } catch (DateTimeParseException e) {
        throw InvalidCursorException.withCursor(cursor);
      }
    }

    List<Conversation> conversations = isAsc
        ? conversationRepository.findPageAsc(currentUserId, keywordLike, cursorTime, idAfter, pageable)
        : conversationRepository.findPageDesc(currentUserId, keywordLike, cursorTime, idAfter, pageable);

    long total = conversationRepository.countAll(currentUserId, keywordLike);

    List<Message> lastestMessages = messageRepository.findLatestMessagesByConversations(conversations);

    Map<UUID, Message> lastestMessageMap = lastestMessages.stream()
        .collect(Collectors.toMap(
            message -> message.getConversation().getId(), message -> message));

    List<Conversation> pageConversations = conversations.size() > limit
        ? conversations.subList(0, limit)
        : conversations;

    List<ConversationDto> dtos = pageConversations.stream()
        .map(conversation -> {
          User otherUser = conversation.getUser1().getId().equals(currentUserId)
              ? conversation.getUser2()
              : conversation.getUser1();
          UserSummary userSummary = userMapper.toUserSummary(otherUser);

          Message lastestMessage = lastestMessageMap.get(conversation.getId());
          DirectMessageDto messageDto = messageMapper.toDto(lastestMessage);

          return conversationMapper.toDto(conversation, userSummary, messageDto);
        })
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (!pageConversations.isEmpty()) {
      Conversation last = pageConversations.get(pageConversations.size() - 1);

      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    boolean hasNext = conversations.size() > limit;

    CursorResponseConversationDto response = new CursorResponseConversationDto(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        total,
        "createdAt",
        sortDirection
    );

    log.info("[Conversation] 대화 조회 완료: total={}", total);

    return response;
  }

  @Transactional(readOnly = true)
  @Override
  public ConversationDto findById(UUID conversationId) {
    log.debug("[Conversation] 대화 조회 시작: {}", conversationId);

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> ConversationNotFoundException.withConversationId(conversationId));

    UUID currentUserId = getCurrentUserId();
    if (!currentUserId.equals(conversation.getUser1().getId()) && !currentUserId.equals(conversation.getUser2().getId())) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    ConversationDto conversationDto = toConversationDto(conversation);

    log.info("[Conversation] 대화 조회 완료: {}", conversationDto.id());
    return conversationDto;
  }

  @Transactional(readOnly = true)
  @Override
  public ConversationDto findByUserId(UUID userId) {
    log.debug("[Conversation] 특정 사용자와의 대화 조회 시작: {}", userId);

    UUID currentUserId = getCurrentUserId();
    Conversation conversation = conversationRepository.findByUserIds(currentUserId, userId)
            .orElseThrow(() -> ConversationNotFoundException.withUserId(userId));

    if (!currentUserId.equals(conversation.getUser1().getId()) && !currentUserId.equals(conversation.getUser2().getId())) {
      throw NotConversationParticipantException.withId(currentUserId);
    }

    ConversationDto conversationDto = toConversationDto(conversation);

    log.info("[Conversation] 특정 사용자와의 대화 조회 완료: {}", conversationDto.id());
    return conversationDto;
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    return userDetails.getUserDto().id();
  }

  private ConversationDto toConversationDto(Conversation conversation) {
    UUID currentUserId = getCurrentUserId();
    User other = currentUserId.equals(conversation.getUser1().getId()) ?
        conversation.getUser2() : conversation.getUser1();
    UserSummary userSummary = userMapper.toUserSummary(other);

    Message lastestMessage = messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation)
        .orElse(null);
    DirectMessageDto messageDto = messageMapper.toDto(lastestMessage);

    ConversationDto conversationDto = conversationMapper.toDto(conversation, userSummary, messageDto);
    return conversationDto;
  }
}
