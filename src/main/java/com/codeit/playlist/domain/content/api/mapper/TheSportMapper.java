package com.codeit.playlist.domain.content.api.mapper;

import com.codeit.playlist.domain.content.api.response.TheSportResponse;
import com.codeit.playlist.domain.content.entity.Content;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TheSportMapper {
    @Mapping(target = "apiId", source = "theSportResponse.idEvent")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "title", source = "theSportResponse.strEvent")
    @Mapping(target = "description", source = "theSportResponse.strFilename")
    @Mapping(target = "thumbnailUrl", source = "theSportResponse.strPoster")
    Content sportsResponseToContent(TheSportResponse theSportResponse, String type);
}
