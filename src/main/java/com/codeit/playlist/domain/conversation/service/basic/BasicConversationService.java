package com.codeit.playlist.domain.conversation.service.basic;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.entity.Message;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public ConversationDto create(ConversationCreateRequest request) {
    log.debug("[Conversation] 대화 생성 시작 {}", request);

//    UUID currentUserId = getCurrentUserId();
//    User currentUser = userRepository.findById(currentUserId)
//        .orElseThrow(() -> UserNotFoundException.withId(currentUserId));
    UUID testCurrentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    if (testCurrentUserId.equals(request.withUserId())){
      throw SelfChatNotAllowedException.withId(testCurrentUserId);
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

  private UUID getCurrentUserId() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    PlaylistUserDetails userDetails = (PlaylistUserDetails) authentication.getPrincipal();
    //    return userDetails.getId();
    return null;
  }
}
