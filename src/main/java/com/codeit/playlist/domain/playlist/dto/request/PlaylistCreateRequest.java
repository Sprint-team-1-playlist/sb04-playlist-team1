package com.codeit.playlist.domain.playlist.dto.request;

public record PlaylistCreateRequest(
        String title,
        String description
) {
}
