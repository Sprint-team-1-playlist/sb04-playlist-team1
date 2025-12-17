package com.codeit.playlist.domain.user.dto.request;

import com.codeit.playlist.domain.user.entity.Role;

public record UserRoleUpdateRequest(
    Role role
) {
}
