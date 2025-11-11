package com.codeit.playlist.global.error;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

  private String code;
  private Map<String, Object> details;
  private String exceptionType;
  private String message;
  private int status;
  private Instant timestamp;

  public ErrorResponse(BusinessException exception) {
    this.code = exception.getErrorCodeName();
    this.details = exception.getDetails();
    this.exceptionType = exception.getClass().getSimpleName();
    this.message = exception.getMessage();
    this.status = exception.getStatus();
    this.timestamp = Instant.now();
  }

  public ErrorResponse(Exception exception, int status) {
    this.code = exception.getClass().getSimpleName();
    this.details = new HashMap<>();
    this.exceptionType = exception.getClass().getSimpleName();
    this.message = exception.getMessage();
    this.status = status;
    this.timestamp = Instant.now();
  }
}