package com.codeit.playlist.message.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.exception.NotConversationParticipantException;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.data.MessageSortBy;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.dto.response.CursorResponseDirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.event.message.DirectMessageSentEvent;
import com.codeit.playlist.domain.message.exception.InvalidMessageReadOperationException;
import com.codeit.playlist.domain.message.exception.MessageNotFoundException;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.service.basic.BasicMessageService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.global.error.InvalidCursorException;
import java.lang.reflect.Field;
import java.security.Principal;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BasicMessageServiceTest {

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private MessageMapper messageMapper;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private BasicMessageService messageService;

  private User user1;
  private User user2;
  private Conversation conversation;
  private UUID conversationId;
  private UUID currentUserId;
  private Principal authentication;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.randomUUID();
    conversationId = UUID.randomUUID();

    user1 = new User("user1@test.com", "pw", "user1", null, Role.USER);
    user2 = new User("user2@test.com", "pw", "user2", null, Role.USER);

    setId(user1, currentUserId);
    setId(user2, UUID.randomUUID());

    conversation = new Conversation(user1, user2);
    setId(conversation, conversationId);

    PlaylistUserDetails userDetails = mock(PlaylistUserDetails.class);
    when(userDetails.getUserDto()).thenReturn(
        new com.codeit.playlist.domain.user.dto.data.UserDto(
            user1.getId(), Instant.now(),
            user1.getEmail(), user1.getName(), null, user1.getRole(), false
        )
    );

    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(userDetails);
    this.authentication = auth;

    SecurityContext securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(auth);

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  @DisplayName("메시지 저장 성공")
  void saveMessageSuccess() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hello!");

    Message savedMessage = new Message(conversation, user1, user2, sendRequest.content());
    setId(savedMessage, UUID.randomUUID());
    setCreatedAt(savedMessage, Instant.now());

    DirectMessageDto dto = new DirectMessageDto(
        savedMessage.getId(),
        conversationId,
        savedMessage.getCreatedAt(),
        new UserSummary(user1.getId(), user1.getName(), user1.getProfileImageUrl()),
        new UserSummary(user2.getId(), user2.getName(), user2.getProfileImageUrl()),
        sendRequest.content()
    );

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageMapper.toDto(savedMessage)).thenReturn(dto);

    // when
    DirectMessageDto result = messageService.save(conversationId, sendRequest, authentication);

    // then
    assertNotNull(result);
    assertEquals(dto.id(), result.id());
    assertEquals(dto.conversationId(), result.conversationId());
    assertEquals(dto.sender().userId(), result.sender().userId());
    assertEquals(dto.receiver().userId(), result.receiver().userId());
    assertEquals(dto.content(), result.content());

    verify(messageRepository, times(1)).save(any(Message.class));
    verify(messageMapper, times(1)).toDto(savedMessage);
    verify(eventPublisher, times(1)).publishEvent(any(DirectMessageSentEvent.class));
  }

  @Test
  @DisplayName("대화방이 존재하지 않으면 예외 발생")
  void saveMessageConversationNotFound() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hello!");
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(ConversationNotFoundException.class, () -> messageService.save(conversationId, sendRequest, authentication));

    verify(messageRepository, never()).save(any());
    verify(messageMapper, never()).toDto(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("sender와 receiver가 올바르게 선택되는지 확인")
  void senderReceiverSelection() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hi there!");

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
      Message m = invocation.getArgument(0);
      setId(m, UUID.randomUUID());
      setCreatedAt(m, Instant.now());
      return m;
    });
    when(messageMapper.toDto(any(Message.class))).thenAnswer(invocation -> {
      Message m = invocation.getArgument(0);
      return new DirectMessageDto(
          m.getId(),
          conversationId,
          m.getCreatedAt(),
          new UserSummary(m.getSender().getId(), m.getSender().getName(), m.getSender().getProfileImageUrl()),
          new UserSummary(m.getReceiver().getId(), m.getReceiver().getName(), m.getReceiver().getProfileImageUrl()),
          m.getContent()
      );
    });

    // when
    DirectMessageDto result = messageService.save(conversationId, sendRequest, authentication);

    // then
    assertEquals(user1.getId(), result.sender().userId());
    assertEquals(user2.getId(), result.receiver().userId());
  }

  @Test
  @DisplayName("메시지 저장 실패 - 현재 유저가 대화 참여자가 아닌 경우")
  void saveMessageNotParticipant() {
    // given
    UUID strangerId = UUID.randomUUID();
    User stranger = new User("stranger@test.com", "pw", "stranger", null, Role.USER);
    setId(stranger, strangerId);

    // Current User를 stranger로 변경
    PlaylistUserDetails userDetails = mock(PlaylistUserDetails.class);
    when(userDetails.getUserDto()).thenReturn(
        new com.codeit.playlist.domain.user.dto.data.UserDto(
            stranger.getId(), Instant.now(),
            stranger.getEmail(), stranger.getName(), null, stranger.getRole(), false
        )
    );
    Authentication strangerAuth = mock(Authentication.class);
    when(strangerAuth.getPrincipal()).thenReturn(userDetails);

    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hello!");
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

    // when & then
    assertThrows(NotConversationParticipantException.class,
        () -> messageService.save(conversationId, sendRequest, strangerAuth));

    verify(messageRepository, never()).save(any());
    verify(messageMapper, never()).toDto(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("대화 메시지 커서 기반 조회 성공")
  void findAllMessagesSuccess() {
    // given
    UUID messageId1 = UUID.randomUUID();
    UUID messageId2 = UUID.randomUUID();

    Instant time1 = Instant.now().minus(10, ChronoUnit.MINUTES);
    Instant time2 = Instant.now().minus(5, ChronoUnit.MINUTES);

    Message m1 = new Message(conversation, user1, user2, "Hello 1");
    setId(m1, messageId1);
    setCreatedAt(m1, time1);

    Message m2 = new Message(conversation, user1, user2, "Hello 2");
    setId(m2, messageId2);
    setCreatedAt(m2, time2);

    DirectMessageDto dto1 = new DirectMessageDto(
        messageId1,
        conversationId,
        time1,
        new UserSummary(user1.getId(), user1.getName(), user1.getProfileImageUrl()),
        new UserSummary(user2.getId(), user2.getName(), user2.getProfileImageUrl()),
        "Hello 1"
    );

    DirectMessageDto dto2 = new DirectMessageDto(
        messageId2,
        conversationId,
        time2,
        new UserSummary(user1.getId(), user1.getName(), user1.getProfileImageUrl()),
        new UserSummary(user2.getId(), user2.getName(), user2.getProfileImageUrl()),
        "Hello 2"
    );

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

    when(messageMapper.toDto(m1)).thenReturn(dto1);
    when(messageMapper.toDto(m2)).thenReturn(dto2);

    List<Message> messages = List.of(m1, m2);

    int limit = 2;
    when(messageRepository.findMessagesByConversationWithCursor(
        any(UUID.class),
        nullable(Instant.class),
        nullable(UUID.class),
        any(Pageable.class)
    )).thenReturn(messages);

    long totalCount = 5L;
    when(messageRepository.countByConversationId(conversationId)).thenReturn(totalCount);

    String cursor = null;
    UUID idAfter = null;

    // when
    CursorResponseDirectMessageDto result = messageService.findAll(conversationId, cursor, idAfter, limit, SortDirection.DESCENDING, MessageSortBy.createdAt, authentication);

    // then
    assertNotNull(result);
    assertEquals(2, result.data().size());
    assertEquals(time2.toString(), result.nextCursor());
    assertEquals(messageId2, result.nextIdAfter());
    assertEquals(true, result.hasNext());
    assertEquals(MessageSortBy.createdAt, result.sortBy());
    assertEquals(SortDirection.DESCENDING, result.sortDirection());
    assertEquals(totalCount, result.totalCount());

    verify(messageRepository, times(1))
        .findMessagesByConversationWithCursor(any(UUID.class), nullable(Instant.class), nullable(UUID.class), any(Pageable.class));

    verify(messageRepository, times(1)).countByConversationId(conversationId);

    verify(messageMapper, times(1)).toDto(m1);
    verify(messageMapper, times(1)).toDto(m2);
  }

  @Test
  @DisplayName("커서 기반 조회 실패 - 대화방이 존재하지 않으면 예외 발생")
  void findAllMessagesConversationNotFound() {
    // given
    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(ConversationNotFoundException.class, () ->
        messageService.findAll(conversationId, null, null, 10, SortDirection.DESCENDING, MessageSortBy.createdAt, authentication)
    );

    verify(messageRepository, never())
        .findMessagesByConversationWithCursor(any(), any(), any(), any());
  }

  @Test
  @DisplayName("커서 기반 조회 실패 - 현재 유저가 대화 참여자가 아니면 예외 발생")
  void findAllMessagesNotParticipant() {
    // given
    UUID strangerId = UUID.randomUUID();
    User stranger = new User("stranger@test.com", "pw", "stranger", null, Role.USER);
    setId(stranger, strangerId);

    PlaylistUserDetails userDetails = mock(PlaylistUserDetails.class);
    when(userDetails.getUserDto()).thenReturn(
        new com.codeit.playlist.domain.user.dto.data.UserDto(
            strangerId, Instant.now(),
            "stranger@test.com", "stranger", null, Role.USER, false
        )
    );

    Authentication strangerAuth = mock(Authentication.class);
    when(strangerAuth.getPrincipal()).thenReturn(userDetails);

    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    // when & then
    assertThrows(NotConversationParticipantException.class, () ->
        messageService.findAll(conversationId, null, null, 10, SortDirection.DESCENDING, MessageSortBy.createdAt, strangerAuth)
    );

    verify(messageRepository, never())
        .findMessagesByConversationWithCursor(any(), any(), any(), any());
  }

  @Test
  @DisplayName("커서 기반 조회 실패 - cursor 형식이 잘못된 경우 InvalidCursorException 발생")
  void findAllMessagesInvalidCursor() {
    // given
    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    String invalidCursor = "not-a-date";

    // when & then
    assertThrows(InvalidCursorException.class, () ->
        messageService.findAll(conversationId, invalidCursor, null, 10, SortDirection.DESCENDING, MessageSortBy.createdAt, authentication)
    );

    verify(messageRepository, never())
        .findMessagesByConversationWithCursor(any(), any(), any(), any());
  }

  @Test
  @DisplayName("DM 읽음 처리 성공")
  void markMessageAsReadSuccess() {
    // given
    UUID messageId = UUID.randomUUID();
    Instant time = Instant.now().minus(10, ChronoUnit.MINUTES);

    Message message = new Message(conversation, user1, user2, "Hello 1");
    setId(message, messageId);
    setCreatedAt(message, time);

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.of(message));

    // when
    messageService.markMessageAsRead(conversationId, messageId, authentication);

    // then
    verify(conversationRepository, times(1)).findById(conversationId);
    verify(messageRepository, times(1)).findById(messageId);
    verify(messageRepository, times(1)).findFirstByConversationOrderByCreatedAtDesc(conversation);
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - Conversation 없음")
  void markMessageAsReadFail_ConversationNotFound() {
    // given
    UUID messageId = UUID.randomUUID();
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(
        ConversationNotFoundException.class,
        () -> messageService.markMessageAsRead(conversationId, messageId, authentication)
    );
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - 현재 유저가 대화 참여자가 아님")
  void markMessageAsReadFail_NotParticipant() {
    // given
    UUID messageId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    User stranger = new User("stranger@test.com", "pw", "stranger", null, Role.USER);
    setId(stranger, strangerId);

    PlaylistUserDetails userDetails = mock(PlaylistUserDetails.class);
    when(userDetails.getUserDto()).thenReturn(
        new com.codeit.playlist.domain.user.dto.data.UserDto(
            strangerId, Instant.now(),
            "stranger@test.com", "stranger", null, Role.USER, false
        )
    );

    Authentication strangerAuth = mock(Authentication.class);
    when(strangerAuth.getPrincipal()).thenReturn(userDetails);

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

    // when & then
    assertThrows(
        NotConversationParticipantException.class,
        () -> messageService.markMessageAsRead(conversationId, messageId, strangerAuth)
    );
    verify(messageRepository, never()).findById(any());
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - Message 없음")
  void markMessageAsReadFail_MessageNotFound() {
    // given
    UUID messageId = UUID.randomUUID();
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(
        MessageNotFoundException.class,
        () -> messageService.markMessageAsRead(conversationId, messageId, authentication)
    );
    verify(messageRepository, times(1)).findById(messageId);
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - 최신 메시지가 아님")
  void markMessageAsReadFail_InvalidMessage() {
    // given
    UUID messageId = UUID.randomUUID();
    Message oldMessage = new Message(conversation, user1, user2, "Old message");
    setId(oldMessage, messageId);

    Message latestMessage = new Message(conversation, user1, user2, "Latest message");
    setId(latestMessage, UUID.randomUUID());

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(oldMessage));
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.of(latestMessage));

    // when & then
    assertThrows(
        InvalidMessageReadOperationException.class,
        () -> messageService.markMessageAsRead(conversationId, messageId, authentication)
    );
    verify(messageRepository, times(1)).findById(messageId);
    verify(messageRepository, times(1)).findFirstByConversationOrderByCreatedAtDesc(conversation);
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

  private void setCreatedAt(BaseEntity entity, Instant time) {
    try {
      Field field = BaseEntity.class.getDeclaredField("createdAt");
      field.setAccessible(true);
      field.set(entity, time);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}