package com.codeit.playlist.domain.user.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.entity.QUser;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final QUser user = QUser.user;

  @Override
  public List<User> searchUsers(String emailLike, String roleEqual, Boolean isLocked, String cursor,
      UUID idAfter, int limit, String sortBy, SortDirection direction) {
    BooleanBuilder builder = baseFilter(emailLike, roleEqual, isLocked);

    // 커서 조건
    if (cursor != null && idAfter != null) {
      builder.and(createCursorCondition(sortBy, direction, cursor, idAfter));
    }

    boolean asc = direction == SortDirection.ASCENDING;

    // 정렬
    OrderSpecifier<?> order = createOrderBy(sortBy, direction);

    return queryFactory
        .selectFrom(user)
        .where(builder)
        .orderBy(order, asc ? user.id.asc() : user.id.desc())
        .limit(limit + 1)
        .fetch();
  }

  @Override
  public long countUsers(String emailLike, String roleEqual, Boolean isLocked) {
    BooleanBuilder builder = baseFilter(emailLike, roleEqual, isLocked);

    Long count = queryFactory
        .select(user.count())
        .from(user)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L;
  }


  private BooleanBuilder baseFilter(String emailLike, String roleEqual, Boolean isLocked) {
    BooleanBuilder builder = new BooleanBuilder();
    if (emailLike != null && !emailLike.isBlank()) {
      builder.and(user.email.like("%" + emailLike + "%"));
    }
    if (roleEqual != null && !roleEqual.isBlank()) {
      try {
        builder.and(user.role.eq(Role.valueOf(roleEqual)));
      } catch (IllegalArgumentException e) {
        //유효하지 않은 role은 무시
      }
    }
    if (isLocked != null) {
      builder.and(user.locked.eq(isLocked));
    }
      return builder;
  }

  private BooleanExpression createCursorCondition(
      String sortBy,
      SortDirection direction,
      String cursor,
      UUID idAfter
  ) {

    boolean asc = direction == SortDirection.ASCENDING;

    return switch (sortBy) {

      case "name" -> asc ?
          user.name.gt(cursor).or(user.name.eq(cursor).and(user.id.gt(idAfter))) :
          user.name.lt(cursor).or(user.name.eq(cursor).and(user.id.lt(idAfter)));

      case "email" -> asc ?
          user.email.gt(cursor).or(user.email.eq(cursor).and(user.id.gt(idAfter))) :
          user.email.lt(cursor).or(user.email.eq(cursor).and(user.id.lt(idAfter)));

      case "createdAt" -> {
        LocalDateTime t;
        try {
          t = LocalDateTime.parse(cursor);
        } catch (DateTimeParseException e) {
          throw InvalidCursorException.withCursor(cursor);
        }

        yield asc ?
            user.createdAt.gt(t).or(user.createdAt.eq(t).and(user.id.gt(idAfter))) :
            user.createdAt.lt(t).or(user.createdAt.eq(t).and(user.id.lt(idAfter)));
      }

      case "role" -> {
        Role role;
        try {
          role = Role.valueOf(cursor);
        } catch (IllegalArgumentException e) {
          throw InvalidCursorException.withCursor(cursor);
        }

        yield asc ?
            user.role.gt(role).or(user.role.eq(role).and(user.id.gt(idAfter))) :
            user.role.lt(role).or(user.role.eq(role).and(user.id.lt(idAfter)));
      }

      case "isLocked" -> {
        if (!cursor.equalsIgnoreCase("true") && !cursor.equalsIgnoreCase("false"))
          throw InvalidCursorException.withCursor(cursor);

        Boolean locked = Boolean.parseBoolean(cursor);

        yield asc ?
            user.locked.gt(locked).or(user.locked.eq(locked).and(user.id.gt(idAfter))) :
            user.locked.lt(locked).or(user.locked.eq(locked).and(user.id.lt(idAfter)));
      }

      default -> {
           LocalDateTime t;
           try {
               t = LocalDateTime.parse(cursor);
             } catch (DateTimeParseException e) {
               throw InvalidCursorException.withCursor(cursor);
             }
        yield asc ?
            user.createdAt.gt(t).or(user.createdAt.eq(t).and(user.id.gt(idAfter))) :
            user.createdAt.lt(t).or(user.createdAt.eq(t).and(user.id.lt(idAfter)));
      }
    };
  }

  private OrderSpecifier<?> createOrderBy(String sortBy, SortDirection direction) {

    boolean asc = direction == SortDirection.ASCENDING;

    return switch (sortBy) {

      case "name" -> asc ? user.name.asc() : user.name.desc();
      case "email" -> asc ? user.email.asc() : user.email.desc();
      case "createdAt" -> asc ? user.createdAt.asc() : user.createdAt.desc();
      case "role" -> asc ? user.role.asc() : user.role.desc();
      case "isLocked" -> asc ? user.locked.asc() : user.locked.desc();

      default -> asc ? user.createdAt.asc() : user.createdAt.desc();
    };
  }
}