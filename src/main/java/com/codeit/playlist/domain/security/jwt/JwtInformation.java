package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.user.dto.data.UserDto;
import java.time.Instant;

public record JwtInformation(UserDto userDto,
                             String accessToken,
                             Instant accessTokenExpiresAt,
                             String refreshToken,
                             Instant refreshTokenExpiresAt) {

}
