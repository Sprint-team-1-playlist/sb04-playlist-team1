package com.codeit.playlist.domain.content.api.mapper;

import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TheMovieMapper {
    @Mapping(target = "type", source = "type")
    @Mapping(target = "title", source = "theMovieResponse.title")
    @Mapping(target = "description", source = "theMovieResponse.description")
    @Mapping(target = "thumbnailUrl", source = "theMovieResponse.thumbnailUrl")
    Content toContent(TheMovieResponse theMovieResponse, Type type);
}
