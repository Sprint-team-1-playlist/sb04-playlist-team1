package com.codeit.playlist.domain.user.dto.data;


import com.codeit.playlist.domain.user.entity.Role;
import java.time.LocalDateTime;

public record UserDto (
    String id,
    LocalDateTime createdAt,
    String email,
    String name,
    String profileImageUrl,
    Role role,
    boolean locked
){

}
