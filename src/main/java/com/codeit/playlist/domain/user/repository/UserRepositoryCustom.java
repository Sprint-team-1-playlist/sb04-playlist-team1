package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

public interface UserRepositoryCustom {
  List<User> searchUsers(
      String emailLike,
      String roleEqual,
      Boolean isLocked,
      String cursor,
      UUID idAfter,
      int limit,
      String sortBy,
      SortDirection direction
  );

  long countUsers(String emailLike, String roleEqual, Boolean isLocked);

}
