package com.codeit.playlist.domain.content.api.mapper;

import com.codeit.playlist.domain.content.api.response.TheSportsResponse;
import com.codeit.playlist.domain.content.entity.Content;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TheSportsMapper {
    @Mapping(target = "type", source = "type")
    @Mapping(target = "description", source = "theSportsResponse.strFilename")
    @Mapping(target = "title", source = "theSportsResponse.strEvent")
    @Mapping(target = "thumbnailUrl", source = "theSportsResponse.strThumb")
    Content sportsResponseToContent(TheSportsResponse theSportsResponse, String type);
}
