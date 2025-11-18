package com.codeit.playlist.message.websocket;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class WebSocketConnectionTest {

  @Test
  @DisplayName("웹소켓 연결 테스트")
  void WebSocketConnectionTest() throws Exception {
    String url = "ws://localhost:8080/ws"; // WebSocket 엔드포인트

    WebSocketStompClient stompClient = new WebSocketStompClient(
        new SockJsClient(
            java.util.List.of(new WebSocketTransport(new StandardWebSocketClient()))
        )
    );

    CompletableFuture<Boolean> connected = new CompletableFuture<>();

    StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
      @Override
      public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        connected.complete(true);
      }

      @Override
      public void handleTransportError(StompSession session, Throwable exception) {
        connected.completeExceptionally(exception);
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        // 메시지 수신 시 처리
      }

      @Override
      public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
      }
    };

    // Spring 6 기준 connectAsync 패턴
    stompClient.connectAsync(url, new WebSocketHttpHeaders(), sessionHandler)
        .whenComplete((session, throwable) -> {
          if (throwable != null) {
            connected.completeExceptionally(throwable);
          }
        });

    // 최대 5초 동안 연결 시도
    boolean isConnected = connected.get(5, TimeUnit.SECONDS);
    assertTrue(isConnected, "WebSocket에 연결되지 않았습니다!");
  }
}
