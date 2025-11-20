package com.codeit.playlist.domain.message.mapper;

import com.codeit.playlist.domain.message.dto.data.DirectMessageDto;
import com.codeit.playlist.domain.message.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring",
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL)
public interface MessageMapper {

  DirectMessageDto toDto(Message message);
}
