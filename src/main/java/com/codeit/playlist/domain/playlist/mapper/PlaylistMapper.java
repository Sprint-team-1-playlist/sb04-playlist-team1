package com.codeit.playlist.domain.playlist.mapper;

import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    // Create 요청 -> 엔티티
    Playlist toEntity(PlaylistCreateRequest request, User owner);

    // owner 없이 생성
    default Playlist toEntity(PlaylistCreateRequest request) {
        return new Playlist(request.title(), request.description());
    }

    // 엔티티 -> DTO
    @Mapping(target = "contents", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "subscribedByMe", constant = "false")
    PlaylistDto toDto(Playlist playlist);
}
