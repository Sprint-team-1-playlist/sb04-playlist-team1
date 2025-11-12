package com.codeit.playlist.domain.user.dto.request;

public record UserCreateRequest (
    String name,
    String email,
    String password
){

}
