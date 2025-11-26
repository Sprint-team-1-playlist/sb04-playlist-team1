package com.codeit.playlist.domain.user.exception;

import java.util.UUID;

public class UserLockStateUnchangedException extends UserException {

  public UserLockStateUnchangedException() {
    super(UserErrorCode.LOCK_STATE_UNCHANGED);
  }

  public static UserLockStateUnchangedException withId(UUID userId) {
    UserLockStateUnchangedException exception = new UserLockStateUnchangedException();
    exception.addDetail("userId", userId);
    return exception;
  }
}
