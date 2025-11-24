package com.codeit.playlist.domain.content.api.mapper;

import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TheMovieMapper {
    Content toContent(TheMovieResponse theMovieResponse, Type type);
}
