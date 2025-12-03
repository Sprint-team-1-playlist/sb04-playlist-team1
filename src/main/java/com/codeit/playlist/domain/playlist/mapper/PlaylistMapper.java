package com.codeit.playlist.domain.playlist.mapper;

import com.codeit.playlist.domain.content.dto.data.ContentSummary;
import com.codeit.playlist.domain.content.mapper.ContentMapper;
import com.codeit.playlist.domain.playlist.dto.data.PlaylistDto;
import com.codeit.playlist.domain.playlist.entity.Playlist;
import com.codeit.playlist.domain.playlist.entity.PlaylistContent;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ContentMapper.class})
public interface PlaylistMapper {

    // 엔티티 -> DTO
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "contents", source = "playlistContents")
    @Mapping(target = "subscribedByMe", constant = "false")
    PlaylistDto toDto(Playlist playlist, @Context Map<UUID, List<String>> tagMap);

    //태그가 필요없는 곳에 쓰일 편의 메서드
    default PlaylistDto toDto(Playlist playlist) {
        return toDto(playlist, java.util.Collections.emptyMap());
    }

    // PlaylistContent → ContentSummary 변환
    @Mapping(target = "id", source = "content.id")
    @Mapping(target = "type", source = "content.type")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "description", source = "content.description")
    @Mapping(target = "thumbnailUrl", source = "content.thumbnailUrl")
    @Mapping(target = "averageRating", source = "content.averageRating")
    @Mapping(target = "reviewCount", source = "content.reviewCount")
    @Mapping(target = "tags", expression = "java(tagMap.getOrDefault(playlistContent.getContent().getId(), java.util.List.of()))")
    ContentSummary toContentSummary(PlaylistContent playlistContent,@Context Map<UUID, List<String>> tagMap);
}
