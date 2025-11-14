package com.codeit.playlist.conversation.service.basic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.mapper.ConversationMapper;
import com.codeit.playlist.domain.conversation.mapper.MessageMapper;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.conversation.repository.MessageRepository;
import com.codeit.playlist.domain.conversation.service.basic.BasicConversationService;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BasicConversationServiceTest {

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private ConversationMapper conversationMapper;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private MessageMapper messageMapper;

  @InjectMocks
  private BasicConversationService conversationService;

  private UUID currentUserId;
  private UUID otherUserId;
  private User currentUser;
  private User otherUser;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    otherUserId = UUID.randomUUID();

    currentUser = new User("current@test.com", "pw", "current", null, Role.USER);
    otherUser = new User("other@test.com", "pw", "other", null, Role.USER);
  }

  @Test
  void createConversationSuccessWithoutMessage() {
    // given
    // 사용자 Mock
    when(userRepository.findById(currentUserId))
        .thenReturn(Optional.of(currentUser));

    when(userRepository.findById(otherUserId))
        .thenReturn(Optional.of(otherUser));

    // 저장될 Conversation
    Conversation savedConversation = new Conversation(currentUser, otherUser);

    // 저장 mock
    when(conversationRepository.save(any()))
        .thenReturn(savedConversation);

    // UserSummary mock
    UserSummary withSummary = new UserSummary(
        otherUserId,
        otherUser.getName(),
        otherUser.getProfileImageUrl()
    );

    when(userMapper.toUserSummary(otherUser))
        .thenReturn(withSummary);

    // mapper 결과 DTO
    ConversationDto dto = new ConversationDto(
        UUID.randomUUID(),    // conversationId
        withSummary,          // 상대방 정보
        null,                 // latestMessage 없음
        false                 // hasUnread
    );

    when(conversationMapper.toDto(
        any(Conversation.class),
        any(UserSummary.class),
        nullable(DirectMessageDto.class)
    )).thenReturn(dto);

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);

    // when
    ConversationDto result = conversationService.create(request);

    // then
    assertNotNull(result);
    assertTrue("lastest message is null as expected", result.lastestMessage() == null);
  }
}
