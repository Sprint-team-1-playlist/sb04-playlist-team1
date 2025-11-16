package com.codeit.playlist.domain.security.jwt;

import com.codeit.playlist.domain.security.PlaylistUserDetails;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  private final int accessTokenExpirationMs;
  private final int refreshTokenExpirationMs;

  private final JWSSigner accessTokenSigner;
  private final JWSVerifier accessTokenVerifier;
  private final JWSSigner refreshTokenSigner;
  private final JWSVerifier refreshTokenVerifier;

  public JwtTokenProvider(
      @Value("${playlist.jwt.access-token.secret}") String accessTokenSecret,
      @Value("${playlist.jwt.access-token.expiration-ms}") int accessTokenExpirationMs,
      @Value("${playlist.jwt.refresh-token.secret}") String refreshTokenSecret,
      @Value("${playlist.jwt.refresh-token.expiration-ms}") int refreshTokenExpirationMs)
      throws JOSEException {

    this.accessTokenExpirationMs = accessTokenExpirationMs;
    this.refreshTokenExpirationMs = refreshTokenExpirationMs;

    byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
    this.accessTokenSigner = new MACSigner(accessSecretBytes);
    this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

    byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
    this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
    this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
  }

  public String generateAccessToken(PlaylistUserDetails user) throws JOSEException {
    return generateToken(user, accessTokenExpirationMs, accessTokenSigner, "access");
  }

  public String generateRefreshToken(PlaylistUserDetails user) throws JOSEException {
    return generateToken(user, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
  }

  public Instant getExpiryFromToken(String token) {
    try {
      SignedJWT parsed = SignedJWT.parse(token);
      Date exp = parsed.getJWTClaimsSet().getExpirationTime();
      return exp.toInstant();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }

  public Instant getAccessTokenExpiryInstant() {
    return Instant.now().plusMillis(accessTokenExpirationMs);
  }

  public Instant getRefreshTokenExpiryInstant() {
    return Instant.now().plusMillis(refreshTokenExpirationMs);
  }

  private String generateToken(PlaylistUserDetails userDetails, int expirationMs, JWSSigner signer,
      String tokenType) throws JOSEException {
    String tokenId = UUID.randomUUID().toString();
    UserDto user = userDetails.getUserDto();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(user.email())
        .jwtID(tokenId)
        .claim("userId", user.id().toString())
        .claim("type", tokenType)
        .claim("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()))
        .issueTime(now)
        .expirationTime(expiryDate)
        .build();

    SignedJWT signedJWT = new SignedJWT(
        new JWSHeader(JWSAlgorithm.HS256),
        claimsSet
    );

    signedJWT.sign(signer);
    String token = signedJWT.serialize();

    log.debug("Generated {} token for user: {}", tokenType, user.name());
    return token;
  }

  public boolean validateAccessToken(String token) {
    return validateToken(token, accessTokenVerifier, "access");
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, refreshTokenVerifier, "refresh");
  }

  private boolean validateToken(String token, JWSVerifier verifier, String expectedType) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      if (!signedJWT.verify(verifier)) {
        log.debug("JWT signature verification failed for {} token", expectedType);
        return false;
      }

      String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
      if (!expectedType.equals(tokenType)) {
        log.debug("JWT token type mismatch: expected {}, got {}", expectedType, tokenType);
        return false;
      }

      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime == null || expirationTime.before(new Date())) {
        log.debug("JWT {} token expired", expectedType);
        return false;
      }

      return true;
    } catch (Exception e) {
      log.debug("JWT {} token validation failed: {}", expectedType, e.getMessage());
      return false;
    }
  }

  public JWTClaimsSet getClaims(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public String getUsernameFromToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet().getSubject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public String getTokenId(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet().getJWTID();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public UUID getUserId(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      String userIdStr = (String) signedJWT.getJWTClaimsSet().getClaim("userId");
      if (userIdStr == null) {
        throw new IllegalArgumentException("User ID claim not found in JWT token");
      }
      return UUID.fromString(userIdStr);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public Cookie genereateRefreshTokenCookie(String refreshToken) {
    Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(false); //Todo: 개발환경에서는 false
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge(refreshTokenExpirationMs / 1000);
    return refreshCookie;
  }

  public Cookie genereateRefreshTokenExpirationCookie() {
    Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(false); //Todo: 개발환경에서는 false
    refreshCookie.setPath("/");
    refreshCookie.setMaxAge(0);
    return refreshCookie;
  }

}
