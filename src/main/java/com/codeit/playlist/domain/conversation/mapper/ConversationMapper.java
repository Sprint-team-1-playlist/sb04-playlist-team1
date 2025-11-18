package com.codeit.playlist.domain.conversation.mapper;

import com.codeit.playlist.domain.conversation.dto.data.ConversationDto;
import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.conversation.entity.Conversation;
import com.codeit.playlist.domain.user.dto.data.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

  @Mapping(source = "conversation.id", target = "id")
  @Mapping(source = "user", target = "with")
  @Mapping(source = "message", target = "lastestMessage")
  @Mapping(source = "conversation.hasUnread", target = "hasUnread")
  ConversationDto toDto(Conversation conversation, UserSummary user, DirectMessageDto message);
}

