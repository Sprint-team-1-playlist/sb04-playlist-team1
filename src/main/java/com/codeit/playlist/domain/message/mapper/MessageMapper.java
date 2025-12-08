package com.codeit.playlist.domain.message.mapper;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring",
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL)
public interface MessageMapper {
  @Mapping(target = "sender.userId", source = "sender.id")
  @Mapping(target = "sender.name", source = "sender.name")
  @Mapping(target = "sender.profileImageUrl", source = "sender.profileImageUrl")
  @Mapping(target = "receiver.userId", source = "receiver.id")
  @Mapping(target = "receiver.name", source = "receiver.name")
  @Mapping(target = "receiver.profileImageUrl", source = "receiver.profileImageUrl")
  @Mapping(target = "conversationId", source = "conversation.id")
  DirectMessageDto toDto(Message message);
}
