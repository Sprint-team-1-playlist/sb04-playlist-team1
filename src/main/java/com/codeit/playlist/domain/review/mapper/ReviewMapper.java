package com.codeit.playlist.domain.review.mapper;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.review.dto.data.ReviewDto;
import com.codeit.playlist.domain.review.dto.request.ReviewCreateRequest;
import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ReviewMapper {

    Review toEntity(ReviewCreateRequest reviewCreateRequest, Content content, User user);

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "author", source = "user")
    ReviewDto toDto(Review review);
}
