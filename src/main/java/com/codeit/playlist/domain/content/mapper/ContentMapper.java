package com.codeit.playlist.domain.content.mapper;

import com.codeit.playlist.domain.content.dto.data.ContentDto;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import com.codeit.playlist.global.constant.S3Properties;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentMapper {
    @Mapping(target = "tags", expression = "java(changeTags(tag))")
    ContentDto toDto(Content content, List<Tag> tag);
    ContentDto toDto(Content content);

    @Mapping(target = "tags", expression = "java(changeTags(tag))")
    @Mapping(target = "thumbnailUrl", expression = "java(toS3Url(content.getThumbnailUrl(), s3Properties))")
    ContentDto toDtoUsingS3(Content content, List<Tag> tag, @Context S3Properties s3Properties);

    default List<String> changeTags(List<Tag> tags) {
        if(tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        return tags.stream().map(Tag::getName)
                .filter(name -> name != null && !name.isEmpty())
                .toList();
    }

    default String toS3Url(String key, S3Properties s3Properties) {
        if(key == null || key.isBlank()) {
            return null;
        }
        if(key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        return "https://" + s3Properties.getContentBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }
}
