package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class UserProfileAccessDeniedException extends UserException {
  public UserProfileAccessDeniedException() {
    super(UserErrorCode.USER_PROFILE_ACCESS_DENIED);
  }

  public static UserProfileAccessDeniedException withId(UUID userId) {
    UserProfileAccessDeniedException exception = new UserProfileAccessDeniedException();
    exception.addDetail("userId", userId);
    return exception;
  }
}
