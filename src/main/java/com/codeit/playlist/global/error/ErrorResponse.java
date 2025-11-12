package com.codeit.playlist.global.error;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

  private String exceptionName;
  private String message;
  private Map<String, Object> details = new HashMap<>();

  public ErrorResponse(BusinessException exception) {
    this.exceptionName = exception.getClass().getSimpleName();
    this.message = exception.getMessage();
    this.details = exception.getDetails();
  }

  public ErrorResponse(Exception exception) {
    this.exceptionName = exception.getClass().getSimpleName();
    this.message = exception.getMessage();
    this.details = new HashMap<>();
  }
}
