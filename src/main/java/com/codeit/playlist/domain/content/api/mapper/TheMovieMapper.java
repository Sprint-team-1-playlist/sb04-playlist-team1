package com.codeit.playlist.domain.content.api.mapper;

import com.codeit.playlist.domain.content.api.response.TheMovieResponse;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TheMovieMapper {
    Content toContent(TheMovieResponse theMovieResponse, Type type);

//    default Type StringtoType(String type) { // String을 Type으로 바꿔주는 Mapper
//        switch(type.toLowerCase()) { // switch문
//            case "movie":
//                return Type.MOVIE;
//            case "tv":
//                return Type.TV_SERIES;
//            default:
//                throw new IllegalArgumentException("Type이 뭔가 이상해요" + type);
//        }
//    }
}
