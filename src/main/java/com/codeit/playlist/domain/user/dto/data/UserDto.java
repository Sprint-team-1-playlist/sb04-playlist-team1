package com.codeit.playlist.domain.user.dto.data;


import java.time.LocalDateTime;

public record UserDto (
    String id,
    LocalDateTime createdAt,
    String email,
    String name,
    String profileImageUrl,
    String role,
    boolean locked
){

}
