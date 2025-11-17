package com.codeit.playlist.conversation.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.conversation.ConversationAlreadyExistsException;
import com.codeit.playlist.domain.conversation.exception.conversation.InvalidCursorException;
import com.codeit.playlist.domain.conversation.exception.conversation.SelfChatNotAllowedException;
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
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
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
    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    otherUserId = UUID.randomUUID();

    currentUser = new User("current@test.com", "pw", "current", null, Role.USER);
    otherUser = new User("other@test.com", "pw", "other", null, Role.USER);

    setId(currentUser, currentUserId);
    setId(otherUser, otherUserId);
  }

  @Test
  @DisplayName("대화 생성 성공")
  void createConversationSuccess() {
    when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
    when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
    when(conversationRepository.findByUserIds(currentUserId, otherUserId)).thenReturn(Optional.empty());

    Conversation savedConversation = new Conversation(currentUser, otherUser);
    setId(savedConversation, UUID.randomUUID());
    when(conversationRepository.save(any())).thenReturn(savedConversation);

    UserSummary withSummary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(withSummary);

    DirectMessageDto lastMessageDto = null;
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
    when(messageMapper.toDto(null)).thenReturn(lastMessageDto);

    ConversationDto dto = new ConversationDto(savedConversation.getId(), withSummary, lastMessageDto, false);
    when(conversationMapper.toDto(any(), any(), nullable(DirectMessageDto.class))).thenReturn(dto);

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);
    ConversationDto result = conversationService.create(request);

    assertNotNull(result);
    assertNull(result.lastestMessage());
  }

  @Test
  @DisplayName("대화 생성 실패 - 자기 자신과 대화 시도")
  void createConversationFailsWhenSelfChat() {
    ConversationCreateRequest request = new ConversationCreateRequest(currentUserId);
    assertThrows(SelfChatNotAllowedException.class, () -> conversationService.create(request));
  }

  @Test
  @DisplayName("대화 생성 실패 - 이미 존재하는 대화")
  void createConversationFailsWhenConversationAlreadyExists() {
    Conversation existingConversation = new Conversation(currentUser, otherUser);
    setId(existingConversation, UUID.randomUUID());
    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(existingConversation));

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);
    assertThrows(ConversationAlreadyExistsException.class, () -> conversationService.create(request));
  }

  @Test
  @DisplayName("대화 목록 조회 - ASC 정렬")
  void findAllAscSortedByCreatedAt() {
    String sortDirection = "ASCENDING";
    int limit = 10;

    Conversation oldConv = new Conversation(currentUser, otherUser);
    Conversation midConv = new Conversation(currentUser, otherUser);
    Conversation newConv = new Conversation(currentUser, otherUser);

    setCreatedAt(oldConv, LocalDateTime.now().minusDays(3));
    setCreatedAt(midConv, LocalDateTime.now().minusDays(2));
    setCreatedAt(newConv, LocalDateTime.now().minusDays(1));

    List<Conversation> sorted = List.of(oldConv, midConv, newConv);
    when(conversationRepository.findPageAsc(currentUserId, null, null, null, PageRequest.of(0, limit+1)))
        .thenReturn(sorted);
    when(conversationRepository.countAll(currentUserId, null)).thenReturn(3L);

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);
    when(conversationMapper.toDto(any(), any(), any())).thenReturn(new ConversationDto(UUID.randomUUID(), summary, null, false));

    CursorResponseConversationDto response = conversationService.findAll(null, null, null, limit, sortDirection, "createdAt");
    assertNotNull(response);
    assertEquals(3, response.data().size());
  }

  @Test
  @DisplayName("대화 목록 조회 - DESC 정렬")
  void findAllDescSortedByCreatedAt() {
    String sortDirection = "DESCENDING";
    int limit = 10;

    Conversation oldConv = new Conversation(currentUser, otherUser);
    Conversation midConv = new Conversation(currentUser, otherUser);
    Conversation newConv = new Conversation(currentUser, otherUser);

    setCreatedAt(oldConv, LocalDateTime.now().minusDays(3));
    setCreatedAt(midConv, LocalDateTime.now().minusDays(2));
    setCreatedAt(newConv, LocalDateTime.now().minusDays(1));

    List<Conversation> sorted = List.of(newConv, midConv, oldConv);
    when(conversationRepository.findPageDesc(currentUserId, null, null, null, PageRequest.of(0, limit+1)))
        .thenReturn(sorted);
    when(conversationRepository.countAll(currentUserId, null)).thenReturn(3L);

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);
    when(conversationMapper.toDto(any(), any(), any())).thenReturn(new ConversationDto(UUID.randomUUID(), summary, null, false));

    CursorResponseConversationDto response = conversationService.findAll(null, null, null, limit, sortDirection, "createdAt");
    assertNotNull(response);
    assertEquals(3, response.data().size());
  }

  @Test
  @DisplayName("대화 목록 조회 실패 - 잘못된 cursor")
  void findAllFailsWhenCursorInvalid() {
    String invalidCursor = "invalid";
    String sortDirection = "ASCENDING";
    assertThrows(InvalidCursorException.class,
        () -> conversationService.findAll(null, invalidCursor, null, 10, sortDirection, "createdAt"));
  }

  private void setCreatedAt(BaseEntity entity, LocalDateTime time) {
    try {
      Field field = BaseEntity.class.getDeclaredField("createdAt");
      field.setAccessible(true);
      field.set(entity, time);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setId(Object entity, UUID id) {
    try {
      Field field = BaseEntity.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
