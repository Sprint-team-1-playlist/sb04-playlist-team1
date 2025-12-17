package com.codeit.playlist.domain.security.jwt;

public record JwtTokens(
    String accessToken,
    String refreshToken
) {

}
