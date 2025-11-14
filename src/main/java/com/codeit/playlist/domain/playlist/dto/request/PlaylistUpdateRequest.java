package com.codeit.playlist.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistUpdateRequest(
        @NotBlank String title,
        @NotBlank String description
) {
}
