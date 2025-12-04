package com.codeit.playlist.domain.user.dto.data;


import com.codeit.playlist.domain.user.entity.Role;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto (
    UUID id,
    LocalDateTime createdAt,
    String email,
    String name,
    String profileImageUrl,
    Role role,
    boolean locked
) implements Serializable {
}
