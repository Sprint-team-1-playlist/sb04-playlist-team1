package com.codeit.playlist.review.repository;

import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Type;
import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.review.repository.ReviewRepository;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.entity.User;
import com.codeit.playlist.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
public class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager em;

    private static final AtomicLong TMDB_ID_SEQ = new AtomicLong(1);

    @Test
    @DisplayName("findReviews 성공 - contentId, 정렬, limit이 정상적으로 적용")
    void findReviewsSuccess() {
        // given
        User user = createTestUser("user@email.com");
        Content content1 = createTestContent("콘텐츠1");
        Content content2 = createTestContent("다른콘텐츠");


        em.persist(user);
        em.persist(content1);
        em.persist(content2);

        // content1 에 대한 리뷰 3개 (createdAt 오름차순: r1 < r2 < r3)
        Review r1 = createReview(user, content1, 3, "리뷰1");
        Review r2 = createReview(user, content1, 4, "리뷰2");
        Review r3 = createReview(user, content1, 5, "리뷰3");

        // content2 에 대한 리뷰 1개 (필터링에서 걸러져야 함)
        Review other = createReview(user, content2, 1, "다른 콘텐츠 리뷰");

        em.persist(r1);
        em.persist(r2);
        em.persist(r3);
        em.persist(other);
        em.flush();

        Instant base = Instant.now();

        // r1 < r2 < r3 (오래된 순)
        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(3, ChronoUnit.DAYS))
                .setParameter("id", r1.getId())
                .executeUpdate();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(2, ChronoUnit.DAYS))
                .setParameter("id", r2.getId())
                .executeUpdate();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(1, ChronoUnit.DAYS))
                .setParameter("id", r3.getId())
                .executeUpdate();

        // 다른 콘텐츠 리뷰는 같은 날로
        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(1, ChronoUnit.DAYS))
                .setParameter("id", other.getId())
                .executeUpdate();

        em.clear();

        int limit = 2;

        // when
        Slice<Review> slice = reviewRepository.findReviews(
                content1.getId(),   // contentId
                null,               // cursor
                null,               // idAfter
                limit,
                SortDirection.DESCENDING,
                "createdAt"
        );

        // then
        List<Review> content = slice.getContent();

        // then - limit 적용
        assertThat(content).hasSize(2);
        // then - hasNext (총 3개 중 2개만 가져왔으니 true)
        assertThat(slice.hasNext()).isTrue();

        // then - 정렬(createdAt DESC) 확인
        Instant first = content.get(0).getCreatedAt();
        Instant second = content.get(1).getCreatedAt();
        assertThat(first).isAfter(second);

        // then - 모두 content1에 대한 리뷰인지 확인
        assertThat(content)
                .allMatch(r -> r.getContent().getId().equals(content1.getId()));
    }

    @Test
    @DisplayName("findReviews 실패 - 해당 콘텐츠에 리뷰가 없으면 빈 Slice를 반환")
    void findReviewsEmptyWhenNoReviewsForContent() {
        // given
        Content content = createTestContent("리뷰 없는 콘텐츠");
        em.persist(content);
        em.flush();
        em.clear();

        int limit = 10;

        // when
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                null,
                null,
                limit,
                SortDirection.DESCENDING,
                "createdAt"
        );

        // then
        assertThat(slice.getContent()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("findReviews 실패 - 잘못된 cursor 문자열이면 커서가 무시되고 첫 페이지가 조회된다")
    void findReviewsIgnoresInvalidCursorString() {
        // given
        User user = createTestUser("cursor@test.com");
        Content content = createTestContent("커서콘텐츠");

        em.persist(user);
        em.persist(content);

        Review r1 = createReview(user, content, 3, "리뷰1");
        Review r2 = createReview(user, content, 4, "리뷰2");

        ReflectionTestUtils.setField(r1, "createdAt", Instant.now().minus(2, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(r2, "createdAt", Instant.now().minus(1, ChronoUnit.DAYS));

        em.persist(r1);
        em.persist(r2);
        em.flush();
        em.clear();

        String invalidCursor = "not-a-uuid";  // UUID 형식이 아님

        // when
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                invalidCursor,   // 잘못된 cursor 문자열
                null,
                10,
                SortDirection.ASCENDING,
                "createdAt"
        );

        // then
        // createCursorCondition 내부에서 UUID 파싱 실패 → cursorId == null → 커서 조건 미적용
        // => 첫 페이지 전체가 그대로 조회됨
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isFalse();
    }

    /**커서 기준 where 조건 생성용 테스트(cursor와 idAfter를 조합해 사용하는 메서드)**/
    @Test
    @DisplayName("createCursorCondition 성공 - createdAt ASC 기준 커서 이후만 조회")
    void findReviewsWithCursorCreatedAtAscSuccess() {
        // given
        User user = createTestUser("cursor-createdAt@test.com");
        Content content = createTestContent("테스트용 컨텐츠");

        em.persist(user);
        em.persist(content);

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // r1 < r2 < r3 (오래된 순)
        Review r1 = createReview(user, content, 3, "리뷰1");
        Review r2 = createReview(user, content, 4, "리뷰2");
        Review r3 = createReview(user, content, 5, "리뷰3");

        em.persist(r1);
        em.persist(r2);
        em.persist(r3);
        em.flush();

        // createdAt: r1(가장 오래됨) < r2 < r3(가장 최신)
        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(3, ChronoUnit.DAYS))
                .setParameter("id", r1.getId())
                .executeUpdate();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(2, ChronoUnit.DAYS))
                .setParameter("id", r2.getId())
                .executeUpdate();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(1, ChronoUnit.DAYS))
                .setParameter("id", r3.getId())
                .executeUpdate();

        em.clear();

        int limit = 10;

        // when
        // 정렬: createdAt ASC
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                null,
                r1.getId(),
                limit,
                SortDirection.ASCENDING,
                "createdAt"
        );

        // then
        List<Review> contentList = slice.getContent();

        assertThat(contentList)
                .extracting(Review::getId)
                .containsExactly(r2.getId(), r3.getId());

        assertThat(contentList.get(0).getCreatedAt())
                .isBefore(contentList.get(1).getCreatedAt());

        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("createCursorCondition 성공 - rating ASC 기준 커서 이후만 조회")
    void findReviewsWithCursorRatingAscSuccess() {
        // given
        User user = createTestUser("cursor-rating@test.com");
        Content content = createTestContent("컨텐츠용 테스트");

        em.persist(user);
        em.persist(content);

        // r1(3점), r2(4점), r3(5점)
        Review r1 = createReview(user, content, 3, "3점 리뷰");
        Review r2 = createReview(user, content, 4, "4점 리뷰");
        Review r3 = createReview(user, content, 5, "5점 리뷰");

        em.persist(r1);
        em.persist(r2);
        em.persist(r3);
        em.flush();
        em.clear();

        int limit = 10;

        // when
        // 정렬: rating ASC → r1(3점), r2(4점), r3(5점)
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                null,
                r1.getId(),
                limit,
                SortDirection.ASCENDING,
                "rating"
        );

        // then
        List<Review> contentList = slice.getContent();

        assertThat(contentList)
                .extracting(Review::getRating)
                .containsExactly(4, 5);

        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("createCursorCondition 실패 - cursorId에 해당하는 리뷰가 없으면 커서 조건이 무시된다")
    void findReviewsWithCursorReviewNotFoundFallsBackToFirstPage() {
        // given
        User user = createTestUser("test@test.com");
        Content content = createTestContent("테스트 컨텐츠");

        em.persist(user);
        em.persist(content);

        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Review r1 = createReview(user, content, 3, "리뷰1");
        Review r2 = createReview(user, content, 4, "리뷰2");

        em.persist(r1);
        em.persist(r2);
        em.flush();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(2, ChronoUnit.DAYS))
                .setParameter("id", r1.getId())
                .executeUpdate();

        em.createQuery("UPDATE Review r SET r.createdAt = :t WHERE r.id = :id")
                .setParameter("t", base.minus(1, ChronoUnit.DAYS))
                .setParameter("id", r2.getId())
                .executeUpdate();

        em.clear();

        int limit = 10;

        UUID nonexistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // when
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                null,
                nonexistentId,           // review 테이블에 없는 cursorId
                limit,
                SortDirection.DESCENDING, // createdAt DESC 기준
                "createdAt"
        );

        // then
        //그냥 첫 페이지 전체가 조회됨
        assertThat(slice.getContent())
                .extracting(Review::getId)
                .containsExactlyInAnyOrder(r1.getId(), r2.getId());

        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    @DisplayName("createCursorCondition 실패 - 지원하지 않는 sortBy 값이면 커서 조건이 적용되지 않는다")
    void findReviewsWithUnsupportedSortByIgnoresCursorCondition() {
        // given
        User user = createTestUser("test1@test.com");
        Content content = createTestContent("테스트 컨텐츠");

        em.persist(user);
        em.persist(content);

        Review r1 = createReview(user, content, 3, "리뷰1");
        Review r2 = createReview(user, content, 4, "리뷰2");

        em.persist(r1);
        em.persist(r2);
        em.flush();
        em.clear();

        int limit = 10;

        // when
        Slice<Review> slice = reviewRepository.findReviews(
                content.getId(),
                null,
                r1.getId(),
                limit,
                SortDirection.DESCENDING,
                "unknown"          // 지원하지 않는 sortBy
        );

        // then
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isFalse();
    }


    private User createTestUser(String email) {
        User user = new User(email, "password", "test-user", null, Role.USER);

        Instant now = Instant.now();
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);

        return user;
    }

    private Content createTestContent(String title) {
        Content content = new Content(
                TMDB_ID_SEQ.getAndIncrement(),
                Type.MOVIE,
                title,
                "설명",
                "http://example.com",
                0L,
                0,
                0
        );

        Instant now = Instant.now();
        ReflectionTestUtils.setField(content, "createdAt", now);
        ReflectionTestUtils.setField(content, "updatedAt", now);

        return content;
    }

    private Review createReview(User user, Content content, int rating, String text) {
        Review review = new Review(content, user, text, rating);

        ReflectionTestUtils.setField(review, "createdAt", Instant.now());
        ReflectionTestUtils.setField(review, "updatedAt", Instant.now());

        return review;
    }

}
