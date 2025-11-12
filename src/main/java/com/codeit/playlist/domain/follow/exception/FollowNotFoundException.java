package com.codeit.playlist.domain.follow.exception;

import com.codeit.playlist.global.constant.ErrorCode;
import java.util.UUID;

public class FollowNotFoundException extends FollowException {

  public FollowNotFoundException() {
    super(FollowErrorCode.FOLLOW_NOT_FOUND);
  }
  public static FollowNotFoundException withId(UUID followId) {
    FollowNotFoundException exception = new FollowNotFoundException();
    exception.addDetail("followId", followId);
    return exception;
  }
}
