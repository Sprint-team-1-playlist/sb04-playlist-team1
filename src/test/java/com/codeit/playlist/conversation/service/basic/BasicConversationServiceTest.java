package com.codeit.playlist.conversation.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.conversation.dto.data.ConversationSortBy;
import com.codeit.playlist.domain.conversation.dto.request.ConversationCreateRequest;
import com.codeit.playlist.domain.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationAlreadyExistsException;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.exception.SelfChatNotAllowedException;
import com.codeit.playlist.domain.conversation.mapper.ConversationMapper;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.conversation.service.basic.BasicConversationService;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.repository.ReadStatusRepository;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.exception.UserNotFoundException;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import com.codeit.playlist.domain.user.repository.UserRepository;
import com.codeit.playlist.global.error.InvalidCursorException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

  @Mock
  private ReadStatusRepository readStatusRepository; // ReadStatusRepository 추가

  @InjectMocks
  private BasicConversationService conversationService;

  private UUID currentUserId;
  private UUID otherUserId;
  private User currentUser;
  private User otherUser;
  private UserSummary otherUserSummary;
  private Conversation existingConversation;
  private UUID conversationId;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    conversationId = UUID.randomUUID();

    currentUser = new User("current@test.com", "pw", "current", "current.png", Role.USER);
    otherUser = new User("other@test.com", "pw", "other", "other.png", Role.USER);

    setId(currentUser, currentUserId);
    setId(otherUser, otherUserId);

    otherUserSummary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());

    existingConversation = new Conversation(currentUser, otherUser);
    setId(existingConversation, conversationId);

    PlaylistUserDetails userDetails = new PlaylistUserDetails(
        new UserDto(currentUser.getId(), Instant.now(), currentUser.getEmail(), currentUser.getName(),
            currentUser.getProfileImageUrl(), currentUser.getRole(), currentUser.isLocked()),
        currentUser.getPassword()
    );

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  @DisplayName("대화 생성 성공")
  void createConversationSuccess() {
    // given

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);

    when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
    when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

    when(conversationRepository.findByUserIds(currentUserId, otherUserId)).thenReturn(Optional.empty());

    Conversation newConversation = new Conversation(currentUser, otherUser);
    setId(newConversation, UUID.randomUUID());

    when(conversationRepository.save(any(Conversation.class))).thenReturn(newConversation);

    DirectMessageDto lastMessageDto = null;

    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(any(Conversation.class))).thenReturn(Optional.empty());

    when(userMapper.toUserSummary(otherUser)).thenReturn(otherUserSummary);
    when(messageMapper.toDto(null)).thenReturn(lastMessageDto);

    ConversationDto expectedDto = new ConversationDto(newConversation.getId(), otherUserSummary, lastMessageDto, false);


    when(conversationMapper.toDto(
        any(Conversation.class),
        eq(otherUserSummary),
        nullable(DirectMessageDto.class),
        anyBoolean())
    ).thenReturn(expectedDto);

    when(readStatusRepository.hasUnread(any(), any())).thenReturn(false);

    // when
    ConversationDto result = conversationService.create(request);

    // then

    assertNotNull(result);
    assertEquals(expectedDto.id(), result.id());
    verify(conversationRepository, times(1)).save(any(Conversation.class));
  }

  @Test
  @DisplayName("대화 생성 실패 - 이미 존재하는 대화")
  void createConversationFailsWhenConversationAlreadyExists() {
    // given

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);

    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(existingConversation));

    // when & then

    assertThrows(ConversationAlreadyExistsException.class, () -> conversationService.create(request));
    verify(conversationRepository, never()).save(any());
  }

  @Test
  @DisplayName("대화 생성 실패 - 자기 자신과의 채팅 시도")
  void createConversationFailsWhenSelfChatAttempted() {
    // given

    ConversationCreateRequest request = new ConversationCreateRequest(currentUserId); // withUserId == currentUserId

    // when & then

    assertThrows(SelfChatNotAllowedException.class, () -> conversationService.create(request));
    verify(conversationRepository, never()).save(any());
  }

  @Test
  @DisplayName("대화 생성 실패 - withUserId에 해당하는 유저가 없음")
  void createConversationFailsWhenOtherUserNotFound() {
    // given

    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);

    when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
    when(userRepository.findById(otherUserId)).thenReturn(Optional.empty()); // Other user not found
    when(conversationRepository.findByUserIds(currentUserId, otherUserId)).thenReturn(Optional.empty());

    // when & then

    assertThrows(UserNotFoundException.class, () -> conversationService.create(request));
    verify(conversationRepository, never()).save(any());
  }

  @Test
  @DisplayName("대화 목록 조회 - ASC 정렬, 커서 없음, hasNext=true")
  void findAllAscSortedByCreatedAtHasNextTrue() {
    // given

    int limit = 2;
    Instant now = Instant.now();

    Conversation conv1 = new Conversation(currentUser, otherUser);
    Conversation conv2 = new Conversation(currentUser, otherUser);
    Conversation conv3 = new Conversation(currentUser, otherUser);

    setId(conv1, UUID.randomUUID());
    setId(conv2, UUID.randomUUID());
    setId(conv3, UUID.randomUUID());

    setCreatedAt(conv1, now.minus(3, ChronoUnit.HOURS));
    setCreatedAt(conv2, now.minus(2, ChronoUnit.HOURS));
    setCreatedAt(conv3, now.minus(1, ChronoUnit.HOURS));

    List<Conversation> foundConversations = List.of(conv1, conv2, conv3); // limit+1 = 3개 반환 (hasNext=true)
    List<Conversation> pageConversations = List.of(conv1, conv2);

    Message m1 = new Message(conv1, currentUser, otherUser, "msg1"); setId(m1, UUID.randomUUID());
    Message m2 = new Message(conv2, currentUser, otherUser, "msg2"); setId(m2, UUID.randomUUID());
    Message m3 = new Message(conv3, currentUser, otherUser, "msg3"); setId(m3, UUID.randomUUID());

    when(conversationRepository.findPageAsc(eq(currentUserId), nullable(String.class), nullable(Instant.class), nullable(UUID.class), any(Pageable.class)))
        .thenReturn(foundConversations);
    when(conversationRepository.countAll(currentUserId, null)).thenReturn(5L);

    when(messageRepository.findLatestMessagesByConversations(foundConversations)).thenReturn(List.of(m1, m2, m3));

    when(userMapper.toUserSummary(otherUser)).thenReturn(otherUserSummary);
    when(messageMapper.toDto(any(Message.class))).thenAnswer(i -> {
      Message m = i.getArgument(0);
      return new DirectMessageDto(m.getId(), m.getConversation().getId(), m.getCreatedAt(), otherUserSummary, otherUserSummary, m.getContent());
    });

    when(readStatusRepository.hasUnread(any(), any())).thenReturn(false);

    when(conversationMapper.toDto(eq(conv1), eq(otherUserSummary), any(DirectMessageDto.class), anyBoolean()))
        .thenReturn(new ConversationDto(conv1.getId(), otherUserSummary, null, false));
    when(conversationMapper.toDto(eq(conv2), eq(otherUserSummary), any(DirectMessageDto.class), anyBoolean()))
        .thenReturn(new ConversationDto(conv2.getId(), otherUserSummary, null, false));

    // when
    CursorResponseConversationDto response = conversationService.findAll(null, null, null, limit, SortDirection.ASCENDING, ConversationSortBy.createdAt);

    // then
    assertNotNull(response);
    assertEquals(limit, response.data().size());
    assertTrue(response.hasNext());
    assertEquals(5L, response.totalCount());

    assertEquals(conv2.getCreatedAt().toString(), response.nextCursor());
    assertEquals(conv2.getId(), response.nextIdAfter());
  }

  @Test
  @DisplayName("대화 목록 조회 실패 - 잘못된 cursor")
  void findAllFailsWhenCursorInvalid() {
    // given

    String invalidCursor = "invalid";

    // when & then
    assertThrows(InvalidCursorException.class,
        () -> conversationService.findAll(null, invalidCursor, null, 10, SortDirection.ASCENDING, ConversationSortBy.createdAt));
    verify(conversationRepository, never()).findPageAsc(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("대화 조회 성공 - 최신 메시지 및 읽음 상태 존재")
  void findByIdSuccessWithLatestMessageAndReadStatus() {
    // given

    UUID conversationId = existingConversation.getId();

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(existingConversation));
    when(userMapper.toUserSummary(otherUser)).thenReturn(otherUserSummary);

    Message latestMessage = new Message(existingConversation, currentUser, otherUser, "hello");
    setId(latestMessage, UUID.randomUUID());
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(existingConversation))
        .thenReturn(Optional.of(latestMessage));

    DirectMessageDto messageDto = new DirectMessageDto(
        latestMessage.getId(), conversationId, Instant.now(),
        new UserSummary(currentUserId, currentUser.getName(), currentUser.getProfileImageUrl()),
        otherUserSummary, "hello"
    );
    when(messageMapper.toDto(latestMessage)).thenReturn(messageDto);

    when(readStatusRepository.hasUnread(conversationId, currentUserId)).thenReturn(true);

    ConversationDto expected = new ConversationDto(conversationId, otherUserSummary, messageDto, true);
    when(conversationMapper.toDto(eq(existingConversation), eq(otherUserSummary), eq(messageDto), eq(true))).thenReturn(expected);

    // when
    ConversationDto result = conversationService.findById(conversationId);

    // then
    assertNotNull(result);
    assertEquals(conversationId, result.id());
    assertTrue(result.hasUnread());
    verify(readStatusRepository, times(1)).hasUnread(conversationId, currentUserId);
  }

  @Test
  @DisplayName("대화 목록 조회 - ASC 정렬, 유효한 커서, hasNext=false")
  void findAllAscSortedByCreatedAtWithValidCursor() {
    // given
    int limit = 2;
    Instant cursorTime = Instant.now().minus(2, ChronoUnit.HOURS);
    String validCursor = cursorTime.toString();
    UUID idAfter = UUID.randomUUID();

    Conversation conv1 = new Conversation(currentUser, otherUser);
    Conversation conv2 = new Conversation(currentUser, otherUser);
    setId(conv1, UUID.randomUUID());
    setId(conv2, UUID.randomUUID());
    setCreatedAt(conv1, cursorTime.minus(1, ChronoUnit.HOURS));
    setCreatedAt(conv2, cursorTime);

    List<Conversation> foundConversations = List.of(conv1, conv2);

    when(conversationRepository.findPageAsc(
        eq(currentUserId),
        nullable(String.class),
        eq(cursorTime),
        eq(idAfter),
        any(Pageable.class)
    )).thenReturn(foundConversations);
    when(conversationRepository.countAll(currentUserId, null)).thenReturn(2L);

    // 나머지 Mocking (메시지 등)은 생략

    // when
    CursorResponseConversationDto response = conversationService.findAll(null, validCursor, idAfter, limit, SortDirection.ASCENDING, ConversationSortBy.createdAt);

    // then
    assertNotNull(response);
    assertFalse(response.hasNext());

    // parseCursor가 호출되고 그 결과가 findPageAsc에 전달되었는지 확인
    verify(conversationRepository, times(1)).findPageAsc(
        any(UUID.class),
        nullable(String.class),
        eq(cursorTime),
        any(UUID.class),
        any(Pageable.class)
    );
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

    Conversation nonParticipantConversation = new Conversation(userA, userB);
    setId(nonParticipantConversation, conversationId);

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(nonParticipantConversation));

    // when & then
    assertThrows(NotConversationParticipantException.class, () -> conversationService.findById(conversationId));
    verify(messageRepository, never()).findFirstByConversationOrderByCreatedAtDesc(any());
  }

  @Test
  @DisplayName("대화 조회 실패 - 대화가 존재하지 않음")
  void findByIdFailsWhenConversationNotFound() {
    // given

    UUID conversationId = UUID.randomUUID();
    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(ConversationNotFoundException.class, () -> conversationService.findById(conversationId));
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 성공 - 최신 메시지가 없는 경우")
  void findByUserIdSuccessWithoutLatestMessage() {
    // given

    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(existingConversation));
    when(userMapper.toUserSummary(otherUser)).thenReturn(otherUserSummary);

    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(existingConversation))
        .thenReturn(Optional.empty());
    when(messageMapper.toDto(null)).thenReturn(null);
    when(readStatusRepository.hasUnread(conversationId, currentUserId)).thenReturn(false);

    ConversationDto expected = new ConversationDto(conversationId, otherUserSummary, null, false);
    when(conversationMapper.toDto(eq(existingConversation), eq(otherUserSummary), nullable(DirectMessageDto.class), eq(false))).thenReturn(expected);

    // when
    ConversationDto result = conversationService.findByUserId(otherUserId);

    // then
    assertNotNull(result);
    assertNull(result.lastestMessage());
    assertFalse(result.hasUnread());
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 실패 - 현재 사용자가 대화 참여자가 아님")
  void findByUserIdFailsWhenNotParticipant() {
    // given
    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(
        ConversationNotFoundException.class,
        () -> conversationService.findByUserId(otherUserId)
    );
  }

  private void setCreatedAt(BaseEntity entity, Instant time) {
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