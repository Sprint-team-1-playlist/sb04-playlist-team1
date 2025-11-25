package com.codeit.playlist.domain.review.repository;

import com.codeit.playlist.domain.review.entity.Review;
import com.codeit.playlist.domain.review.repository.custom.ReviewRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

    // 콘텐츠 기준 전체 개수
    long countByContent_Id(UUID contentId);
}
