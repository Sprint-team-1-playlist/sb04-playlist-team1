package com.codeit.playlist.domain.follow.exception;

import java.util.UUID;

public class FollowAlreadyExistsException extends FollowException{

  public FollowAlreadyExistsException() {
    super(FollowErrorCode.FOLLOW_ALREADY_EXISTS);
  }
  public static FollowAlreadyExistsException withId(UUID followeeId) {
    FollowAlreadyExistsException exception = new FollowAlreadyExistsException();
    exception.addDetail("followeeId", followeeId);
    return exception;
  }
}
