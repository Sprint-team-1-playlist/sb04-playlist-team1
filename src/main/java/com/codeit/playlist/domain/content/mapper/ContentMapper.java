package com.codeit.playlist.domain.content.mapper;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentMapper {
    @Mapping(target = "tags", expression = "java(changeTags(tag))")
    ContentDto toDto(Content content, List<Tag> tag);
    ContentDto toDto(Content content);
    Content toEntity(ContentDto contentDto);

    default List<String> changeTags(List<Tag> tags) {
        List<String> resultTag = new ArrayList<>();
        resultTag.addAll(
                tags.stream().map(Tag::getName)
                        .filter(name -> name != null && !name.isEmpty())
                        .toList()
        );
        return resultTag;
    }
}
