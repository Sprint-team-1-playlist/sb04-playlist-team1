package com.codeit.playlist.domain.user.mapper;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "profileImageUrl", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "followCount", ignore = true)
  User toEntity(UserCreateRequest request);

  UserDto toDto(User user);

}
