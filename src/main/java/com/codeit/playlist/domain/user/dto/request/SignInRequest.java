package com.codeit.playlist.domain.user.dto.request;

public record SignInRequest(
    String username,
    String password
) {

}
