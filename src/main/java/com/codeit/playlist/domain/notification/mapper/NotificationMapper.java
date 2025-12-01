package com.codeit.playlist.domain.notification.mapper;

import com.codeit.playlist.domain.notification.dto.data.NotificationDto;
import com.codeit.playlist.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "receiverId", source = "receiver.id")
    NotificationDto toDto(Notification notification);

    List<NotificationDto> toDtoList(List<Notification> notifications);
}
