package com.codeit.playlist.domain.playlist.mapper;

import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ContentMapper.class})
public interface PlaylistMapper {

    // 엔티티 -> DTO
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "contents", source = "playlistContents")
    @Mapping(target = "subscribedByMe", constant = "false")
    PlaylistDto toDto(Playlist playlist);
}
