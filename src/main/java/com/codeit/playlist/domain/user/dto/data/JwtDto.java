package com.codeit.playlist.domain.user.dto.data;

public record JwtDto (
  UserDto userDto,
  String accessToken
){
}
