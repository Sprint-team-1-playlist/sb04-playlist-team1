package com.codeit.playlist.domain.conversation.service.basic;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.entity.Message;
import com.codeit.playlist.domain.conversation.exception.conversation.ConversationAlreadyExistsException;
import com.codeit.playlist.domain.conversation.exception.conversation.SelfChatNotAllowedException;
import com.codeit.playlist.domain.conversation.mapper.ConversationMapper;
import com.codeit.playlist.domain.conversation.mapper.MessageMapper;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.conversation.repository.MessageRepository;
import com.codeit.playlist.domain.conversation.service.ConversationService;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

//    UUID currentUserId = getCurrentUserId();
//    User currentUser = userRepository.findById(currentUserId)
//        .orElseThrow(() -> UserNotFoundException.withId(currentUserId));
    UUID testCurrentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    if (testCurrentUserId.equals(request.withUserId())){
      throw SelfChatNotAllowedException.withId(testCurrentUserId);
    }

    Optional<Conversation> existingConversation = conversationRepository
        .findByUserIds(testCurrentUserId, request.withUserId());
    if (existingConversation.isPresent()) {
      throw ConversationAlreadyExistsException.withId(existingConversation.get().getId());
    }

    User currentUser = userRepository.findById(testCurrentUserId)
        .orElseThrow(() -> UserNotFoundException.withId(testCurrentUserId));
    User user = userRepository.findById(request.withUserId())
        .orElseThrow(() -> UserNotFoundException.withId(request.withUserId()));

    Conversation conversation = new Conversation(currentUser,user);
    conversationRepository.save(conversation);

    UserSummary userSummary = userMapper.toUserSummary(user);

    Message latestMessage = messageRepository
        .findFirstByConversationOrderByCreatedAtDesc(conversation)
        .orElse(null);
    DirectMessageDto messageDto = messageMapper.toDto(latestMessage);

    ConversationDto conversationDto = conversationMapper.toDto(conversation, userSummary, messageDto);

    log.info("[Conversation] 대화 생성 완료 {}", conversationDto);
    return conversationDto;
  }

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

    Pageable pageable = PageRequest.of(0, limit);

    UUID currentUserId = getCurrentUserId();

    LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;

    List<Conversation> conversations = isAsc
        ? conversationRepository.findPageAsc(currentUserId, keywordLike, cursorTime, idAfter, pageable)
        : conversationRepository.findPageDesc(currentUserId, keywordLike, cursorTime, idAfter, pageable);

    long total = conversationRepository.countAll(currentUserId, keywordLike);

    List<ConversationDto> dtos = conversations.stream()
        .map(conversation -> {
          User otherUser = conversation.getUser1().getId().equals(currentUserId)
              ? conversation.getUser2()
              : conversation.getUser1();
          UserSummary userSummary = userMapper.toUserSummary(otherUser);

          Message latestMessage = messageRepository
              .findFirstByConversationOrderByCreatedAtDesc(conversation)
              .orElse(null);

          DirectMessageDto msgDto = messageMapper.toDto(latestMessage);

          return conversationMapper.toDto(conversation, userSummary, msgDto);
        })
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;

    if (!conversations.isEmpty()) {
      Conversation last = conversations.get(conversations.size() - 1);

      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
      hasNext = (conversations.size() == limit);
    }

    CursorResponseConversationDto response = new CursorResponseConversationDto(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        total,
        "createdAt",
        sortDirection
    );

    log.info("[Conversation] 대화 조회 완료: {}", response);

    return response;
  }

  private UUID getCurrentUserId() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    //    return userDetails.getId();
    return UUID.fromString("11111111-1111-1111-1111-111111111111");
  }
}
