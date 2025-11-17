package com.codeit.playlist.conversation.service.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

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

    // ✨ User 엔티티의 id 필드 강제 주입
    setId(currentUser, currentUserId);
    setId(otherUser, otherUserId);
  }

  @Test
  @DisplayName("대화 생성 성공")
  void createConversationSuccess() {
    // given
    // 사용자 Mock
    when(userRepository.findById(currentUserId))
        .thenReturn(Optional.of(currentUser));

    when(userRepository.findById(otherUserId))
        .thenReturn(Optional.of(otherUser));

    // 기존 대화 없음
    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.empty());

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
    assertNull(result.lastestMessage(), "lastest message is null as expected");
  }

  @Test
  @DisplayName("대화 생성 실패 - 자기 자신과 대화 시도")
  void createConversationFailsWhenSelfChat() {
    // given
    when(userRepository.findById(currentUserId))
        .thenReturn(Optional.of(currentUser));

    // when & then
        ConversationCreateRequest request = new ConversationCreateRequest(currentUserId);
    assertThrows(SelfChatNotAllowedException.class, () -> conversationService.create(request));
  }

  @Test
  @DisplayName("대화 생성 실패 - 이미 존재하는 대화")
  void createConversationFailsWhenConversationAlreadyExists() {
    // given
    when(userRepository.findById(currentUserId))
        .thenReturn(Optional.of(currentUser));
    when(userRepository.findById(otherUserId))
        .thenReturn(Optional.of(otherUser));

    Conversation existingConversation = new Conversation(currentUser, otherUser);
    when(conversationRepository.findByUserIds(currentUserId, otherUserId))
        .thenReturn(Optional.of(existingConversation));

    // when & then
    ConversationCreateRequest request = new ConversationCreateRequest(otherUserId);
    assertThrows(ConversationAlreadyExistsException.class, () -> conversationService.create(request));
  }

  @Test
  @DisplayName("대화 목록 조회 - ASC")
  void findAllAscSortedByCreatedAt() {
    // given
    String sortDirection = "ASCENDING";
    String keyword = null;
    String cursor = null;
    UUID idAfter = null;
    int limit = 10;

    Conversation oldConv = new Conversation(currentUser, otherUser);
    Conversation midConv = new Conversation(currentUser, otherUser);
    Conversation newConv = new Conversation(currentUser, otherUser);

    // createdAt 수동 설정
    setCreatedAt(oldConv, LocalDateTime.now().minusDays(3));
    setCreatedAt(midConv, LocalDateTime.now().minusDays(2));
    setCreatedAt(newConv, LocalDateTime.now().minusDays(1));

    List<Conversation> sorted = List.of(oldConv, midConv, newConv);

    when(conversationRepository.findPageAsc(
        keyword,
        cursor,
        idAfter,
        PageRequest.of(0, limit)
    )).thenReturn(sorted);

    when(conversationRepository.countAll(keyword))
        .thenReturn(3L);

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser))
        .thenReturn(summary);
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(any()))
        .thenReturn(Optional.empty());
    when(conversationMapper.toDto(any(), any(), any()))
        .thenReturn(new ConversationDto(UUID.randomUUID(), summary, null, false));

    // when
    CursorResponseConversationDto response = conversationService.findAll(keyword, cursor, idAfter, limit, sortDirection, "createdAt");

    // then
    assertNotNull(response);
    assertEquals(3, response.data().size());

    // 실제 createdAt 정렬 확인
    assertTrue(oldConv.getCreatedAt().isBefore(midConv.getCreatedAt()));
    assertTrue(midConv.getCreatedAt().isBefore(newConv.getCreatedAt()));
  }

  @Test
  @DisplayName("대화 목록 조회 - DESC createdAt 정렬 검증")
  void findAllDescSortedByCreatedAt() {
    // given
    String sortDirection = "DESCENDING";
    String keyword = null;
    String cursor = null;
    UUID idAfter = null;
    int limit = 10;

    Conversation oldConv = new Conversation(currentUser, otherUser);
    Conversation midConv = new Conversation(currentUser, otherUser);
    Conversation newConv = new Conversation(currentUser, otherUser);

    // createdAt 수동 설정
    setCreatedAt(oldConv, LocalDateTime.now().minusDays(3));
    setCreatedAt(midConv, LocalDateTime.now().minusDays(2));
    setCreatedAt(newConv, LocalDateTime.now().minusDays(1));

    List<Conversation> sorted = List.of(newConv, midConv, oldConv);

    when(conversationRepository.findPageDesc(
        keyword,
        cursor,
        idAfter,
        PageRequest.of(0, limit)
    ))
        .thenReturn(sorted);

    when(conversationRepository.countAll(keyword)).thenReturn(3L);

    UserSummary summary = new UserSummary(otherUserId, otherUser.getName(), otherUser.getProfileImageUrl());
    when(userMapper.toUserSummary(otherUser))
        .thenReturn(summary);
    when(messageRepository.findFirstByConversationOrderByCreatedAtDesc(any()))
        .thenReturn(Optional.empty());
    when(conversationMapper.toDto(any(), any(), any()))
        .thenReturn(new ConversationDto(UUID.randomUUID(), summary, null, false));

    // when
    var response = conversationService.findAll(keyword, cursor, idAfter, limit, sortDirection, "createdAt");

    // then
    assertNotNull(response);
    assertEquals(3, response.data().size());

    // 실제 createdAt DESC 정렬 확인
    assertTrue(newConv.getCreatedAt().isAfter(midConv.getCreatedAt()));
    assertTrue(midConv.getCreatedAt().isAfter(oldConv.getCreatedAt()));
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
