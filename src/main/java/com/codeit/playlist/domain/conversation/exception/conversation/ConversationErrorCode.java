package com.codeit.playlist.domain.conversation.exception.conversation;

import com.codeit.playlist.global.constant.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConversationErrorCode implements ErrorCode {
  CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "채팅방을 찾을 수 없습니다."),
  SELF_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "자기 자신과 채팅방을 생성할 수 없습니다."),
  CONVERSATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST.value(), "같은 채팅방을 생성할 수 없습니다.");

  private final int status;
  private final String message;

  @Override
  public String getErrorCodeName() {
    return name();
  }
}
