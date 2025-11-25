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
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationAlreadyExistsException;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.mapper.ConversationMapper;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.conversation.service.basic.BasicConversationService;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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

    PlaylistUserDetails userDetails = new PlaylistUserDetails(
        new UserDto(currentUser.getId(), LocalDateTime.now(), currentUser.getEmail(), currentUser.getName(),
            currentUser.getProfileImageUrl(), currentUser.getRole(), currentUser.isLocked()),
        currentUser.getPassword()
    );

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
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

    ConversationDto dto = new ConversationDto(savedConversation.getId(), withSummary, lastMessageDto, false);
    when(conversationMapper.toDto(any(), any(), nullable(DirectMessageDto.class))).thenReturn(dto);

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);
    ConversationDto result = conversationService.create(request);

    assertNotNull(result);
    assertNull(result.lastestMessage());
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

  @Test
  @DisplayName("대화 조회 성공 - 최신 메시지가 존재하는 경우")
  void findByIdSuccessWithLatestMessage() {
    // given
    UUID conversationId = UUID.randomUUID();

    Conversation conversation = new Conversation(currentUser, otherUser);
    setId(conversation, conversationId);

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);

    Message latestMessage = new Message(conversation, currentUser, otherUser, "hello");
    setId(latestMessage, UUID.randomUUID());
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.of(latestMessage));

    DirectMessageDto messageDto = new DirectMessageDto(
        UUID.randomUUID(),
        conversationId,
        LocalDateTime.now(),
        new UserSummary(currentUserId, currentUser.getName(), currentUser.getProfileImageUrl()),
        new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl()),
        "hello"
    );

    when(messageMapper.toDto(latestMessage)).thenReturn(messageDto);

    ConversationDto expected = new ConversationDto(conversationId, summary, messageDto, false);
    when(conversationMapper.toDto(conversation, summary, messageDto)).thenReturn(expected);

    // when
    ConversationDto result = conversationService.findById(conversationId);

    // then
    assertNotNull(result);
    assertEquals(conversationId, result.id());
    assertEquals(messageDto, result.lastestMessage());
  }

  @Test
  @DisplayName("대화 조회 성공 - 최신 메시지가 없는 경우")
  void findByIdSuccessWithoutLatestMessage() {
    // given
    UUID conversationId = UUID.randomUUID();

    Conversation conversation = new Conversation(currentUser, otherUser);
    setId(conversation, conversationId);

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);

    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.empty());

    when(messageMapper.toDto(null)).thenReturn(null);

    ConversationDto expected = new ConversationDto(conversationId, summary, null, false);
    when(conversationMapper.toDto(conversation, summary, null)).thenReturn(expected);

    // when
    ConversationDto result = conversationService.findById(conversationId);

    // then
    assertNotNull(result);
    assertNull(result.lastestMessage());
  }

  @Test
  @DisplayName("대화 조회 실패 - 현재 사용자가 대화 참여자가 아닌 경우")
  void findByIdFailsWhenNotParticipant() {
    // given
    UUID conversationId = UUID.randomUUID();

    User userA = new User("a@test.com", "pw", "A", null, Role.USER);
    User userB = new User("b@test.com", "pw", "B", null, Role.USER);
    setId(userA, UUID.randomUUID());
    setId(userB, UUID.randomUUID());

    Conversation conversation = new Conversation(userA, userB);
    setId(conversation, conversationId);

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    // when & then
    assertThrows(NotConversationParticipantException.class, () -> conversationService.findById(conversationId));
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 성공 - 최신 메시지가 존재하는 경우")
  void findByUserIdSuccessWithLatestMessage() {
    // given
    Conversation conversation = new Conversation(currentUser, otherUser);
    UUID convId = UUID.randomUUID();
    setId(conversation, convId);

    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(conversation));

    // user summary
    UserSummary summary = new UserSummary(
        otherUserId,
        otherUser.getName(),
        otherUser.getProfileImageUrl()
    );
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);

    // latest message
    Message latest = new Message(conversation, currentUser, otherUser, "hello");
    setId(latest, UUID.randomUUID());
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.of(latest));

    DirectMessageDto msgDto = new DirectMessageDto(
        UUID.randomUUID(),
        convId,
        LocalDateTime.now(),
        new UserSummary(currentUserId, currentUser.getName(), currentUser.getProfileImageUrl()),
        new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl()),
        "hello"
    );
    when(messageMapper.toDto(latest)).thenReturn(msgDto);

    ConversationDto expectedDto = new ConversationDto(convId, summary, msgDto, false);
    when(conversationMapper.toDto(conversation, summary, msgDto)).thenReturn(expectedDto);

    // when
    ConversationDto result = conversationService.findByUserId(otherUserId);

    // then
    assertNotNull(result);
    assertEquals(convId, result.id());
    assertEquals(msgDto, result.lastestMessage());

    assertNotNull(result.with());
    assertEquals(otherUserId, result.with().userId());
    assertEquals(otherUser.getName(), result.with().name());
    assertEquals(otherUser.getProfileImageUrl(), result.with().profileImageUrl());
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 성공 - 최신 메시지가 없는 경우")
  void findByUserIdSuccessWithoutLatestMessage() {
    // given
    Conversation conversation = new Conversation(currentUser, otherUser);
    setId(conversation, UUID.randomUUID());

    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(conversation));

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser)).thenReturn(summary);

    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.empty());

    ConversationDto expected = new ConversationDto(conversation.getId(), summary, null, false);
    when(conversationMapper.toDto(conversation, summary, null)).thenReturn(expected);

    // when
    ConversationDto result = conversationService.findByUserId(otherUserId);

    // then
    assertNotNull(result);
    assertNull(result.lastestMessage());

    assertNotNull(result.with());
    assertEquals(otherUserId, result.with().userId());
    assertEquals(otherUser.getName(), result.with().name());
    assertEquals(otherUser.getProfileImageUrl(), result.with().profileImageUrl());
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 실패 - 대화가 존재하지 않음")
  void findByUserIdFailsWhenConversationNotFound() {
    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.empty());

    assertThrows(
        com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException.class,
        () -> conversationService.findByUserId(otherUserId)
    );
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 실패 - 현재 사용자가 대화 참여자가 아님")
  void findByUserIdFailsWhenNotParticipant() {
    // given
    User someone1 = new User("u1@test.com", "pw", "u1", null, Role.USER);
    User someone2 = new User("u2@test.com", "pw", "u2", null, Role.USER);

    UUID s1Id = UUID.randomUUID();
    UUID s2Id = UUID.randomUUID();
    setId(someone1, s1Id);
    setId(someone2, s2Id);

    Conversation conversation = new Conversation(someone1, someone2);
    setId(conversation, UUID.randomUUID());

    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(conversation));

    // when & then
    assertThrows(NotConversationParticipantException.class,
        () -> conversationService.findByUserId(otherUserId));
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
