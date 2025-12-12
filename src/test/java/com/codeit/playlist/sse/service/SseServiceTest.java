package com.codeit.playlist.sse.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.playlist.domain.sse.entity.SseMessage;
import com.codeit.playlist.domain.sse.exception.InvalidEventNameException;
import com.codeit.playlist.domain.sse.exception.SseReconnectFailedException;
import com.codeit.playlist.domain.sse.exception.SseSendFailedException;
import com.codeit.playlist.domain.sse.repository.SseEmitterRepository;
import com.codeit.playlist.domain.sse.repository.SseMessageRepository;
import com.codeit.playlist.domain.sse.service.SseService;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class SseServiceTest {

  @InjectMocks
  private SseService sseService;

  @Mock
  private SseEmitterRepository sseEmitterRepository;

  @Mock
  private SseMessageRepository sseMessageRepository;

  private final UUID TEST_RECEIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final UUID TEST_OTHER_RECEIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private final UUID TEST_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
  private final String TEST_EVENT_NAME = "testEvent";
  private final Object TEST_DATA = Map.of("message", "hello");

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(sseService, "timeout", 300000L);
  }

  @Test
  @DisplayName("connect_lastEventId_reconnect: 저장된 메시지 재전송 성공")
  void connect_success_withLastEventId() throws IOException {
    // given
    UUID lastEventId = TEST_EVENT_ID;

    SseMessage mockMessage1 = mock(SseMessage.class);
    SseMessage mockMessage2 = mock(SseMessage.class);

    when(mockMessage1.toEvent()).thenReturn(Set.of(new DataWithMediaType("msg1", null)));
    when(mockMessage2.toEvent()).thenReturn(Set.of(new DataWithMediaType("msg2", null)));

    List<SseMessage> unsentMessages = List.of(mockMessage1, mockMessage2);

    when(sseMessageRepository.findAllByEventIdAfterAndReceiverId(lastEventId, TEST_RECEIVER_ID))
        .thenReturn(unsentMessages);

    SseEmitter mockEmitter = mock(SseEmitter.class);
    when(sseEmitterRepository.save(eq(TEST_RECEIVER_ID), any(SseEmitter.class))).thenReturn(mockEmitter);

    // when
    SseEmitter emitter = sseService.connect(TEST_RECEIVER_ID, lastEventId);

    // then
    verify(sseEmitterRepository, times(1)).save(eq(TEST_RECEIVER_ID), eq(emitter));

    verify(mockMessage1, times(1)).toEvent();
    verify(mockMessage2, times(1)).toEvent();
  }

  @Test
  @DisplayName("connect_failure_nullReceiverId: receiverId가 null일 때 예외 발생")
  void connect_failure_nullReceiverId() {
    // when, then
    assertThrows(SseReconnectFailedException.class,
        () -> sseService.connect(null, null));

    // then
    verify(sseEmitterRepository, never()).save(any(), any());
  }

  @Test
  @DisplayName("send_success: 메시지 전송 성공")
  void send_success() throws IOException {
    // given
    Collection<UUID> receiverIds = List.of(TEST_RECEIVER_ID, TEST_OTHER_RECEIVER_ID);
    SseMessage mockMessage = mock(SseMessage.class);
    Set<DataWithMediaType> mockEvent = Set.of(new DataWithMediaType("test data", null));
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    Map<UUID, SseEmitter> emitterMap = Map.of(
        TEST_RECEIVER_ID, emitter1,
        TEST_OTHER_RECEIVER_ID, emitter2
    );

    when(sseMessageRepository.save(any(SseMessage.class))).thenReturn(mockMessage);
    when(mockMessage.toEvent()).thenReturn(mockEvent);
    when(sseEmitterRepository.findAllByReceiverIdsIn(receiverIds)).thenReturn(emitterMap);

    doNothing().when(emitter1).send(any(Set.class));
    doNothing().when(emitter2).send(any(Set.class));


    // when
    sseService.send(receiverIds, TEST_EVENT_NAME, TEST_DATA);

    // then
    verify(sseMessageRepository, times(1)).save(any(SseMessage.class));
    verify(emitter1, times(1)).send(eq(mockEvent));
    verify(emitter2, times(1)).send(eq(mockEvent));
    assertDoesNotThrow(() -> sseService.send(receiverIds, TEST_EVENT_NAME, TEST_DATA));
  }

  @Test
  @DisplayName("send_emptyReceiverIds: receiverIds가 비어있을 때 메서드 정상 종료")
  void send_emptyReceiverIds() {
    // given
    Collection<UUID> emptyReceiverIds = Collections.emptyList();

    // when
    sseService.send(emptyReceiverIds, TEST_EVENT_NAME, TEST_DATA);

    // then
    verify(sseMessageRepository, never()).save(any());
    verify(sseEmitterRepository, never()).findAllByReceiverIdsIn(any());
    assertDoesNotThrow(() -> sseService.send(emptyReceiverIds, TEST_EVENT_NAME, TEST_DATA));
  }

  @Test
  @DisplayName("send_failure_invalidEventName: eventName이 null이거나 비어있을 때 예외 발생")
  void send_failure_invalidEventName() {
    // given
    Collection<UUID> receiverIds = List.of(TEST_RECEIVER_ID);

    // when, then
    assertThrows(InvalidEventNameException.class,
        () -> sseService.send(receiverIds, null, TEST_DATA));
    assertThrows(InvalidEventNameException.class,
        () -> sseService.send(receiverIds, "", TEST_DATA));

    // then
    verify(sseMessageRepository, never()).save(any());
  }

  @Test
  @DisplayName("send_failure_someEmittersFailed: 메시지 전송 중 일부 Emitter에서 예외 발생")
  void send_failure_someEmittersFailed() throws IOException {
    // given
    Collection<UUID> receiverIds = List.of(TEST_RECEIVER_ID, TEST_OTHER_RECEIVER_ID);
    SseMessage mockMessage = mock(SseMessage.class);
    Set<DataWithMediaType> mockEvent = Set.of(new DataWithMediaType("test data", null));
    SseEmitter successEmitter = mock(SseEmitter.class);
    SseEmitter failedEmitter = mock(SseEmitter.class);
    Map<UUID, SseEmitter> emitterMap = Map.of(
        TEST_RECEIVER_ID, successEmitter,
        TEST_OTHER_RECEIVER_ID, failedEmitter
    );

    when(sseMessageRepository.save(any(SseMessage.class))).thenReturn(mockMessage);
    when(mockMessage.toEvent()).thenReturn(mockEvent);
    doThrow(new IOException("Test Send Failed")).when(failedEmitter).send(any(Set.class));
    when(sseEmitterRepository.findAllByReceiverIdsIn(receiverIds)).thenReturn(emitterMap);
    when(mockMessage.getEventId()).thenReturn(TEST_EVENT_ID);

    // when, then
    SseSendFailedException exception = assertThrows(SseSendFailedException.class,
        () -> sseService.send(receiverIds, TEST_EVENT_NAME, TEST_DATA));

    // then
    verify(successEmitter, times(1)).send(eq(mockEvent));
    verify(failedEmitter, times(1)).send(any(Set.class));

    String exceptionMessage = exception.getMessage();

    boolean containsKeyWord = exceptionMessage.contains("failed") || exceptionMessage.contains("실패");
    assertTrue(containsKeyWord, "예외 메시지에 실패 관련 키워드('failed'/'실패')가 포함되어야 합니다.");
  }

  @Test
  @DisplayName("broadcast_success: 전체 Emitter에 메시지 전송 성공")
  void broadcast_success() throws IOException {
    // given
    SseMessage mockMessage = mock(SseMessage.class);
    Set<DataWithMediaType> mockEvent = Set.of(new DataWithMediaType("test data", null));
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    List<SseEmitter> allEmitters = List.of(emitter1, emitter2);

    when(sseMessageRepository.save(any(SseMessage.class))).thenReturn(mockMessage);
    when(mockMessage.toEvent()).thenReturn(mockEvent);
    when(sseEmitterRepository.findAll()).thenReturn(allEmitters);

    doNothing().when(emitter1).send(any(Set.class));
    doNothing().when(emitter2).send(any(Set.class));

    // when
    sseService.broadcast(TEST_EVENT_NAME, TEST_DATA);

    // then
    verify(sseMessageRepository, times(1)).save(any(SseMessage.class));
    verify(emitter1, times(1)).send(eq(mockEvent));
    verify(emitter2, times(1)).send(eq(mockEvent));
  }

  @Test
  @DisplayName("broadcast_failure_invalidEventName: eventName이 null이거나 비어있을 때 예외 발생")
  void broadcast_failure_invalidEventName() {
    // when, then
    assertThrows(InvalidEventNameException.class,
        () -> sseService.broadcast(null, TEST_DATA));
    assertThrows(InvalidEventNameException.class,
        () -> sseService.broadcast("", TEST_DATA));

    // then
    verify(sseMessageRepository, never()).save(any());
  }

  @Test
  @DisplayName("broadcast_partialFailure: 메시지 전송 중 일부 Emitter에서 예외 발생")
  void broadcast_partialFailure() throws IOException {
    // given
    SseMessage mockMessage = mock(SseMessage.class);
    Set<DataWithMediaType> mockEvent = Set.of(new DataWithMediaType("test data", null));
    SseEmitter successEmitter = mock(SseEmitter.class);
    SseEmitter failedEmitter1 = mock(SseEmitter.class);
    SseEmitter failedEmitter2 = mock(SseEmitter.class);
    List<SseEmitter> allEmitters = List.of(successEmitter, failedEmitter1, failedEmitter2);

    when(sseMessageRepository.save(any(SseMessage.class))).thenReturn(mockMessage);
    when(mockMessage.toEvent()).thenReturn(mockEvent);
    when(sseEmitterRepository.findAll()).thenReturn(allEmitters);

    doThrow(new IOException("Test Broadcast Failed 1")).when(failedEmitter1).send(any(Set.class));
    doThrow(new IOException("Test Broadcast Failed 2")).when(failedEmitter2).send(any(Set.class));
    doNothing().when(successEmitter).send(any(Set.class));

    // when
    sseService.broadcast(TEST_EVENT_NAME, TEST_DATA);

    // then
    verify(successEmitter, times(1)).send(eq(mockEvent));
    verify(failedEmitter1, times(1)).send(any(Set.class));
    verify(failedEmitter2, times(1)).send(any(Set.class));
  }

  @Test
  @DisplayName("cleanUp_success: 죽은 Emitter 정리 성공 (ping 실패 시뮬레이션)")
  void cleanUp_success() throws IOException {
    // given
    SseEmitter deadEmitter = mock(SseEmitter.class);
    SseEmitter aliveEmitter = mock(SseEmitter.class);
    List<SseEmitter> allEmitters = List.of(deadEmitter, aliveEmitter);

    when(sseEmitterRepository.findAll()).thenReturn(allEmitters);

    doThrow(new IOException("Dead ping"))
        .when(deadEmitter).send(any(Set.class));
    doNothing()
        .when(aliveEmitter).send(any(Set.class));

    // when
    sseService.cleanUp();

    // then
    verify(deadEmitter, times(1)).complete();
    verify(aliveEmitter, never()).complete();
  }

  @Test
  @DisplayName("keepAlive_success: Emitter에 keep-alive 메시지 전송 성공")
  void keepAlive_success() throws IOException {
    // given
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    List<SseEmitter> allEmitters = List.of(emitter1, emitter2);

    when(sseEmitterRepository.findAll()).thenReturn(allEmitters);

    doNothing().when(emitter1).send(any(SseEmitter.SseEventBuilder.class));
    doNothing().when(emitter2).send(any(SseEmitter.SseEventBuilder.class));

    // when
    sseService.keepAlive();

    // then
    verify(emitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter1, never()).complete();
    verify(emitter2, never()).complete();
  }

  @Test
  @DisplayName("keepAlive_failure: Emitter send 실패 시 complete 호출")
  void keepAlive_failure() throws IOException {
    // given
    SseEmitter emitterFailed = mock(SseEmitter.class);
    List<SseEmitter> allEmitters = List.of(emitterFailed);

    when(sseEmitterRepository.findAll()).thenReturn(allEmitters);
    doThrow(new IOException("Keep-alive failed"))
        .when(emitterFailed).send(any(SseEmitter.SseEventBuilder.class));

    // when
    sseService.keepAlive();

    // then
    verify(emitterFailed, times(1)).complete();
    verify(emitterFailed, times(1)).send(any(SseEmitter.SseEventBuilder.class));
  }
}