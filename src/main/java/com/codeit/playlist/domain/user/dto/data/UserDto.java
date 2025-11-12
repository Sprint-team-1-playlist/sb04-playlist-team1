package com.codeit.playlist.domain.user.dto.data;


public record UserDto (
    String id,
    String createdAt,
    String email,
    String name,
    String profileImageUrl,
    String role,
    boolean locked
){

}
