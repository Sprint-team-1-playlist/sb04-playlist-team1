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
