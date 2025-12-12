package com.codeit.playlist.user.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.domain.user.repository.UserRepositoryCustom;
import com.codeit.playlist.domain.user.repository.UserRepositoryCustomImpl;
import com.codeit.playlist.global.error.InvalidCursorException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@Import({UserRepositoryCustomImpl.class,
    UserRepositoryCustomImplTest.QuerydslTestConfig.class})
@EnableJpaAuditing
class UserRepositoryCustomImplTest {

  @TestConfiguration
  static class QuerydslTestConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
      return new JPAQueryFactory(em);
    }
  }

  @Autowired
  private TestEntityManager em;

  @Autowired
  private UserRepositoryCustom userRepositoryCustom;

  private UUID id1;
  private UUID id2;
  private Instant t1;
  private Instant t2;

  @BeforeEach
  void setUp() {
    t1 = Instant.parse("2024-01-01T00:00:00Z");
    t2 = Instant.parse("2024-01-02T00:00:00Z");

    User u1 = new User("a@test.com", "pw", "AAA", null, Role.USER);
    User u2 = new User("b@test.com", "pw", "BBB", null, Role.ADMIN);

    em.persist(u1);
    em.persist(u2);
    em.flush();

    id1 = u1.getId();
    id2 = u2.getId();
  }

  @Test
  @DisplayName("searchUsers - emailLike 값으로 이메일 부분 검색")
  void searchUsersEmailLike() {
    List<User> result = userRepositoryCustom.searchUsers(
        "a@", null, null,
        null, null,
        10, "email", SortDirection.ASCENDING
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).contains("a@");
  }

  @Test
  @DisplayName("searchUsers - roleEqual 값으로 역할 필터링")
  void searchUsersRoleEqual() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, "ADMIN", null,
        null, null,
        10, "email", SortDirection.ASCENDING
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  @DisplayName("searchUsers - 유효하지 않은 roleEqual 값은 무시됨")
  void searchUsersInvalidRoleIgnored() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, "INVALID_ROLE", null,
        null, null,
        10, "email", SortDirection.ASCENDING
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("searchUsers - isLocked 값으로 잠금 상태 필터링")
  void searchUsersIsLocked() {
    em.getEntityManager()
        .createQuery("update User u set u.locked = true where u.email = 'a@test.com'")
        .executeUpdate();
    em.flush();
    em.clear();

    List<User> result = userRepositoryCustom.searchUsers(
        null, null, true,
        null, null,
        10, "email", SortDirection.ASCENDING
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isLocked()).isTrue();
  }

  @Test
  @DisplayName("searchUsers - createdAt 기준 커서 페이징 정상 동작")
  void searchUsersCreatedAtCursor() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        t1.toString(), id1,
        10, "createdAt", SortDirection.ASCENDING
    );

    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("searchUsers - createdAt 커서 파싱 실패 시 InvalidCursorException 발생")
  void searchUsersInvalidCreatedAtCursor() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "invalid-date", id1,
            10, "createdAt", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("searchUsers - role 커서가 enum 값이 아니면 InvalidCursorException 발생")
  void searchUsersInvalidRoleCursor() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "NOT_A_ROLE", id1,
            10, "role", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("searchUsers - isLocked 커서가 boolean 값이 아니면 InvalidCursorException 발생")
  void searchUsersInvalidLockedCursor() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "notBoolean", id1,
            10, "isLocked", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("searchUsers - 알 수 없는 sortBy 값이면 createdAt 기준 기본 정렬")
  void searchUsersDefaultSortBy() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        null, null,
        10, "unknown",
        SortDirection.ASCENDING
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("countUsers - 조건 없이 전체 사용자 수를 반환한다")
  void countUsersNoFilter() {
    // when
    long count = userRepositoryCustom.countUsers(null, null, null);

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=name ASC 커서 조건")
  void cursorConditionNameAsc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "AAA", id1,
        10, "name", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=name DESC 커서 조건")
  void cursorConditionNameDesc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "BBB", id2,
        10, "name", SortDirection.DESCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=email ASC 커서 조건")
  void cursorConditionEmailAsc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "a@test.com", id1,
        10, "email", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=email DESC 커서 조건")
  void cursorConditionEmailDesc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "b@test.com", id2,
        10, "email", SortDirection.DESCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=createdAt ASC 정상 커서")
  void cursorConditionCreatedAtAsc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        Instant.now().toString(), id1,
        10, "createdAt", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=createdAt 잘못된 커서면 예외")
  void cursorConditionCreatedAtInvalid() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "invalid-date", id1,
            10, "createdAt", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=role ASC 정상 커서")
  void cursorConditionRoleAsc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "USER", id1,
        10, "role", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=role 잘못된 커서면 예외")
  void cursorConditionRoleInvalid() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "NOT_A_ROLE", id1,
            10, "role", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=isLocked ASC 정상 커서")
  void cursorConditionIsLockedAsc() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        "true", id1,
        10, "isLocked", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("createCursorCondition - sortBy=isLocked 잘못된 커서면 예외")
  void cursorConditionIsLockedInvalid() {
    assertThatThrownBy(() ->
        userRepositoryCustom.searchUsers(
            null, null, null,
            "notBoolean", id1,
            10, "isLocked", SortDirection.ASCENDING
        )
    ).isInstanceOf(InvalidCursorException.class);
  }

  @Test
  @DisplayName("createCursorCondition - 알 수 없는 sortBy면 createdAt 기본 분기")
  void cursorConditionDefault() {
    List<User> result = userRepositoryCustom.searchUsers(
        null, null, null,
        Instant.now().toString(), id1,
        10, "unknown", SortDirection.ASCENDING
    );

    assertThat(result).isNotNull();
  }

}
