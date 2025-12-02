package com.codeit.playlist.domain.user.exception;

public class UserNameRequiredException extends UserException {
  public UserNameRequiredException() {
    super(UserErrorCode.USER_NAME_REQUIRED);
  }

  public static UserNameRequiredException withUserName(String userName) {
    UserNameRequiredException exception = new UserNameRequiredException();
    exception.addDetail("userName", userName);
    return exception;

  }
}
