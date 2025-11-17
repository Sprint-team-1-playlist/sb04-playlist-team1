package com.codeit.playlist.domain.playlist.mapper;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PlaylistMapper {

    // Create 요청 -> 엔티티
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "subscriberCount", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Playlist toEntity(PlaylistCreateRequest request, User owner);

    // 엔티티 -> DTO
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "contents", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "subscribedByMe", constant = "false")
    PlaylistDto toDto(Playlist playlist);
}
