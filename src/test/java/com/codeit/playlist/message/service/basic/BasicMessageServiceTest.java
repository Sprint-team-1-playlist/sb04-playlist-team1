package com.codeit.playlist.message.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
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

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

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
  @DisplayName("메시지 저장 성공 - 현재 유저가 conversation.user2일 때 sender/receiver 할당 확인")
  void senderReceiverSelectionWhenUserIsUser2() {
    // given
    Principal user2Auth = setupUser2AsCurrentPrincipal();
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("User2 says hello!");

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
    DirectMessageDto result = messageService.save(conversationId, sendRequest, user2Auth);

    // then
    assertEquals(user2.getId(), result.sender().userId(), "user2가 발신자여야 합니다.");
    assertEquals(user1.getId(), result.receiver().userId(), "user1이 수신자여야 합니다.");

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository, times(1)).save(messageCaptor.capture());

    Message capturedMessage = messageCaptor.getValue();
    assertEquals(user2.getId(), capturedMessage.getSender().getId(), "저장된 메시지의 발신자는 user2여야 합니다.");
    assertEquals(user1.getId(), capturedMessage.getReceiver().getId(), "저장된 메시지의 수신자는 user1이어야 합니다.");
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
  @DisplayName("메시지 저장 성공 - 30자 초과 메시지에 대해 알림 미리보기가 '...'로 잘리는지 확인")
  void saveMessageLongContentNotificationPreview() throws Exception {
    // given
    String longContent = "This is a very long message content that definitely exceeds thirty characters in length.";
    String expectedPreview = "This is a very long message co...";
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest(longContent);

    Message savedMessage = new Message(conversation, user1, user2, sendRequest.content());
    setId(savedMessage, UUID.randomUUID());
    setCreatedAt(savedMessage, Instant.now());

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageMapper.toDto(any(Message.class))).thenReturn(mock(DirectMessageDto.class));

    String kafkaPayload = "{\"content\":\"" + expectedPreview + "\", ...}";
    when(objectMapper.writeValueAsString(any())).thenReturn(kafkaPayload);

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

    // when
    messageService.save(conversationId, sendRequest, authentication);

    // then
    verify(kafkaTemplate, times(1)).send(org.mockito.ArgumentMatchers.eq("playlist.NotificationDto"), payloadCaptor.capture());

    ArgumentCaptor<com.codeit.playlist.domain.notification.dto.data.NotificationDto> notificationDtoCaptor = ArgumentCaptor.forClass(com.codeit.playlist.domain.notification.dto.data.NotificationDto.class);
    verify(objectMapper, times(1)).writeValueAsString(notificationDtoCaptor.capture());

    assertEquals(expectedPreview, notificationDtoCaptor.getValue().content());
  }

  @Test
  @DisplayName("메시지 저장 성공 - 알림 직렬화 실패 시 예외 처리 확인 (Kafka 전송 안 됨)")
  void saveMessageJsonProcessingException() throws Exception {
    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Short message");

    Message savedMessage = new Message(conversation, user1, user2, sendRequest.content());
    setId(savedMessage, UUID.randomUUID());
    setCreatedAt(savedMessage, Instant.now());

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageMapper.toDto(any(Message.class))).thenReturn(mock(DirectMessageDto.class));

    when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test serialization failure") {});

    // when
    DirectMessageDto result = messageService.save(conversationId, sendRequest, authentication);

    // then
    assertNotNull(result);
    verify(kafkaTemplate, never()).send(any(), any());

    verify(messageRepository, times(1)).save(any(Message.class));
    verify(eventPublisher, times(1)).publishEvent(any(DirectMessageSentEvent.class));
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
  @DisplayName("DM 목록 조회 성공 - 다음 페이지 존재 시, limit+1개 조회 후 limit개로 잘리는지 확인 (hasNext=true)")
  void findAllMessagesSuccessWithNextPage() {
    // given
    int limit = 2;

    UUID messageId1 = UUID.randomUUID();
    UUID messageId2 = UUID.randomUUID();
    UUID messageId3 = UUID.randomUUID();

    Instant time1 = Instant.now().minus(10, ChronoUnit.MINUTES);
    Instant time2 = Instant.now().minus(5, ChronoUnit.MINUTES);
    Instant time3 = Instant.now().minus(1, ChronoUnit.MINUTES);

    Message m1 = createMessage(messageId1, time1, "Hello 1");
    Message m2 = createMessage(messageId2, time2, "Hello 2");
    Message m3 = createMessage(messageId3, time3, "Hello 3");

    DirectMessageDto dto1 = createDto(messageId1, time1, "Hello 1");
    DirectMessageDto dto2 = createDto(messageId2, time2, "Hello 2");

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageMapper.toDto(m1)).thenReturn(dto1);
    when(messageMapper.toDto(m2)).thenReturn(dto2);

    // DESC 가정(최신→과거)에 맞춰 정렬
    List<Message> messagesFromRepo = List.of(m3, m2, m1);

    when(messageRepository.findMessagesByConversationWithCursor(
        any(UUID.class),
        nullable(Instant.class),
        nullable(UUID.class),
        any(Pageable.class)
    )).thenReturn(messagesFromRepo);

    long totalCount = 5L;
    when(messageRepository.countByConversationId(conversationId)).thenReturn(totalCount);

    // when
    CursorResponseDirectMessageDto result = messageService.findAll(conversationId, null, null, limit, SortDirection.DESCENDING, "createdAt", authentication);

    // then
    assertNotNull(result);

    assertEquals(limit, result.data().size(), "조회된 메시지 수는 limit과 같아야 합니다.");

    assertEquals(true, result.hasNext(), "limit+1개가 조회되었으므로 hasNext는 true여야 합니다.");

    assertEquals(time2.toString(), result.nextCursor());
    assertEquals(messageId2, result.nextIdAfter());

    verify(messageMapper, times(1)).toDto(m3);
    verify(messageMapper, times(1)).toDto(m2);
    verify(messageMapper, never()).toDto(m1);
  }

  @Test
  @DisplayName("DM 목록 조회 성공 - 다음 페이지 없음 시, 정확히 limit개 조회 후 그대로 반환되는지 확인 (hasNext=false)")
  void findAllMessagesSuccessNoNextPage_ExactLimit() {
    // given
    int limit = 2;

    UUID messageId1 = UUID.randomUUID();
    UUID messageId2 = UUID.randomUUID();

    Instant time1 = Instant.now().minus(10, ChronoUnit.MINUTES);
    Instant time2 = Instant.now().minus(5, ChronoUnit.MINUTES);

    Message m1 = createMessage(messageId1, time1, "Hello 1");
    Message m2 = createMessage(messageId2, time2, "Hello 2");

    DirectMessageDto dto1 = createDto(messageId1, time1, "Hello 1");
    DirectMessageDto dto2 = createDto(messageId2, time2, "Hello 2");

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageMapper.toDto(m1)).thenReturn(dto1);
    when(messageMapper.toDto(m2)).thenReturn(dto2);

    List<Message> messagesFromRepo = List.of(m2, m1);

    when(messageRepository.findMessagesByConversationWithCursor(
        any(UUID.class),
        nullable(Instant.class),
        nullable(UUID.class),
        any(Pageable.class)
    )).thenReturn(messagesFromRepo);

    long totalCount = (long) messagesFromRepo.size();
    when(messageRepository.countByConversationId(conversationId)).thenReturn(totalCount);

    // when
    CursorResponseDirectMessageDto result = messageService.findAll(conversationId, null, null, limit, SortDirection.DESCENDING, "createdAt", authentication);

    // then
    assertNotNull(result);

    assertEquals(limit, result.data().size(), "조회된 메시지 수는 limit과 같아야 합니다.");

    assertEquals(false, result.hasNext(), "정확히 limit개만 조회되었고 totalCount가 같으므로 hasNext는 false여야 합니다.");

    assertEquals(time1.toString(), result.nextCursor());
    assertEquals(messageId1, result.nextIdAfter());
  }

  @Test
  @DisplayName("커서 기반 조회 실패 - 대화방이 존재하지 않으면 예외 발생")
  void findAllMessagesConversationNotFound() {
    // given
    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(ConversationNotFoundException.class, () ->
        messageService.findAll(conversationId, null, null, 10, SortDirection.DESCENDING, "createdAt", authentication)
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
        messageService.findAll(conversationId, null, null, 10, SortDirection.DESCENDING, "createdAt", strangerAuth)
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
        messageService.findAll(conversationId, invalidCursor, null, 10, SortDirection.DESCENDING, "createdAt", authentication)
    );

    verify(messageRepository, never())
        .findMessagesByConversationWithCursor(any(), any(), any(), any());
  }

  @Test
  @DisplayName("커서 기반 조회 성공 - 유효한 cursor가 Instant로 파싱되는지 확인")
  void findAllMessagesWithValidCursor() {
    // given
    when(conversationRepository.findById(conversationId))
        .thenReturn(Optional.of(conversation));

    final Instant fixedValidTime = Instant.parse("2025-12-10T10:00:00.123456789Z");
    String validCursor = fixedValidTime.toString();

    int limit = 10;
    List<Message> messages = List.of();

    when(messageRepository.findMessagesByConversationWithCursor(
        any(UUID.class),
        any(Instant.class),
        nullable(UUID.class),
        any(Pageable.class)
    )).thenReturn(messages);
    when(messageRepository.countByConversationId(conversationId)).thenReturn(0L);

    // when
    messageService.findAll(conversationId, validCursor, null, limit, SortDirection.DESCENDING, "createdAt", authentication);

    // then
    verify(messageRepository, times(1))
        .findMessagesByConversationWithCursor(
            any(UUID.class),
            argThat(instant -> instant.equals(fixedValidTime)),
            nullable(UUID.class),
            any(Pageable.class)
        );
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
  void markMessageAsReadFailConversationNotFound() {
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
  void markMessageAsReadFailNotParticipant() {
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
  void markMessageAsReadFailMessageNotFound() {
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
  void markMessageAsReadFailInvalidMessage() {
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

  @Test
  @DisplayName("DM 읽음 처리 실패 - 대화방에 메시지가 전혀 없을 때")
  void markMessageAsReadFailWhenNoMessagesInConversation() {
    // given
    UUID messageId = UUID.randomUUID();
    Message message = new Message(conversation, user1, user2, "The only message");
    setId(message, messageId);

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation))
        .thenReturn(Optional.empty()); // 대화방에 최신 메시지가 없음

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

  private Message createMessage(UUID id, Instant time, String content) {
    Message message = new Message(conversation, user1, user2, content);
    setId(message, id);
    setCreatedAt(message, time);
    return message;
  }

  private DirectMessageDto createDto(UUID id, Instant time, String content) {
    return new DirectMessageDto(
        id,
        conversationId,
        time,
        new UserSummary(user1.getId(), user1.getName(), user1.getProfileImageUrl()),
        new UserSummary(user2.getId(), user2.getName(), user2.getProfileImageUrl()),
        content
    );
  }

  //user2를 인증된 사용자로 설정
  private Principal setupUser2AsCurrentPrincipal() {
    UUID user2Id = user2.getId();

    PlaylistUserDetails userDetails = mock(PlaylistUserDetails.class);
    when(userDetails.getUserDto()).thenReturn(
        new com.codeit.playlist.domain.user.dto.data.UserDto(
            user2Id, Instant.now(),
            user2.getEmail(), user2.getName(), null, user2.getRole(), false
        )
    );

    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(userDetails);

    return auth;
  }
}
