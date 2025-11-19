package com.codeit.playlist.message.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.conversation.exception.ConversationNotFoundException;
import com.codeit.playlist.domain.conversation.repository.ConversationRepository;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.dto.request.DirectMessageSendRequest;
import com.codeit.playlist.domain.message.entity.Message;
import com.codeit.playlist.domain.message.mapper.MessageMapper;
import com.codeit.playlist.domain.message.repository.MessageRepository;
import com.codeit.playlist.domain.message.service.basic.BasicMessageService;
import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
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

  @InjectMocks
  private BasicMessageService messageService;

  private User user1;
  private User user2;
  private Conversation conversation;
  private UUID conversationId;
  private UUID currentUserId;

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
            user1.getId(), LocalDateTime.now(),
            user1.getEmail(), user1.getName(), null, user1.getRole(), false
        )
    );

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);

    SecurityContext securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);
  }


  @Test
  @DisplayName("메시지 저장 성공")
  void saveMessageSuccess() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hello!");

    Message savedMessage = new Message(conversation, user1, user2, sendRequest.content());

    DirectMessageDto dto = new DirectMessageDto(
        UUID.randomUUID(),
        conversationId,
        LocalDateTime.now(),
        new UserSummary(user1.getId(), user1.getName(), user1.getProfileImageUrl()),
        new UserSummary(user2.getId(), user2.getName(), user2.getProfileImageUrl()),
        sendRequest.content()
    );

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
    when(messageMapper.toDto(savedMessage)).thenReturn(dto);

    // when
    DirectMessageDto result = messageService.save(conversationId, sendRequest);

    // then
    assertNotNull(result);
    assertEquals(dto.id(), result.id());
    assertEquals(dto.conversationId(), result.conversationId());
    assertEquals(dto.sender().userId(), result.sender().userId());
    assertEquals(dto.receiver().userId(), result.receiver().userId());
    assertEquals(dto.content(), result.content());

    verify(messageRepository, times(1)).save(any(Message.class));
    verify(messageMapper, times(1)).toDto(savedMessage);
  }

  @Test
  @DisplayName("대화방이 존재하지 않으면 예외 발생")
  void saveMessageConversationNotFound() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hello!");
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(ConversationNotFoundException.class, () -> messageService.save(conversationId, sendRequest));

    verify(messageRepository, never()).save(any());
    verify(messageMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("sender와 receiver가 올바르게 선택되는지 확인")
  void senderReceiverSelection() {

    // given
    DirectMessageSendRequest sendRequest = new DirectMessageSendRequest("Hi there!");

    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(messageMapper.toDto(any(Message.class))).thenAnswer(invocation -> {
      Message m = invocation.getArgument(0);
      return new DirectMessageDto(
          m.getId(),
          conversationId,
          LocalDateTime.now(),
          new UserSummary(m.getSender().getId(), m.getSender().getName(), m.getSender().getProfileImageUrl()),
          new UserSummary(m.getReceiver().getId(), m.getReceiver().getName(), m.getReceiver().getProfileImageUrl()),
          m.getContent()
      );
    });

    // when
    DirectMessageDto result = messageService.save(conversationId, sendRequest);

    // then
    assertEquals(user1.getId(), result.sender().userId());
    assertEquals(user2.getId(), result.receiver().userId());
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
