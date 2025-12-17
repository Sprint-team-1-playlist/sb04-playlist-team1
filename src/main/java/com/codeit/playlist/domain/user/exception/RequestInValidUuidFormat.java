package com.codeit.playlist.domain.user.exception;

public class RequestInValidUuidFormat extends UserException {

  public RequestInValidUuidFormat() {
    super(UserErrorCode.REQUEST_INVALID_UUID_FORMAT);
  }

  public static RequestInValidUuidFormat withId(String id) {
    RequestInValidUuidFormat exception = new RequestInValidUuidFormat();
    exception.addDetail("id", id);
    return exception;

  }
}
