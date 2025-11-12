package com.codeit.playlist.domain.follow.exception;

import java.util.UUID;

public class FollowSelfNotAllowedException extends FollowException {

  public FollowSelfNotAllowedException() {
    super(FollowErrorCode.FOLLOW_SELF_NOT_ALLOWED);
  }
  public static FollowSelfNotAllowedException withId(UUID followeeId) {
    FollowSelfNotAllowedException exception = new FollowSelfNotAllowedException();
    exception.addDetail("followeeId", followeeId);
    return exception;
  }
}
