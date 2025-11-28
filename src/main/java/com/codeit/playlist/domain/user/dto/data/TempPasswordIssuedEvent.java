package com.codeit.playlist.domain.user.dto.data;

public record TempPasswordIssuedEvent(
    String email,
    String tempPassword
) {

}
