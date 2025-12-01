package com.codeit.playlist.domain.content.mapper;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentMapper {
    ContentDto toDto(Content content, List<Tag> tag);

    Content toEntity(ContentDto contentDto);

//    @Named("stringToList")
//    default List<String> stringToList(String tag) {
//        if(tag == null || tag.isBlank()) {
//            return List.of();
//        }
//        return Arrays.stream(tag.split(","))
//                .map(String::trim)
//                .toList();
//    }
//
//    @Named("listToString")
//    default String listToString(List<String> tags) {
//        if(tags == null || tags.isEmpty()) {
//            return "";
//        }
//        return String.join(",", tags);
//    }

}
