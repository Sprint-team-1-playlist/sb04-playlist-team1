package com.codeit.playlist.domain.content.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

    /**
     * TMDB ID
     */
    @Column(name = "api_id", nullable = false)
    private Long apiId;

    /**
     * 컨텐츠 타입
     */
    @Column(length = 20, nullable = false)
    private String type;

    /**
     * 컨텐츠 제목
     */
    @Column(length = 100, nullable = false)
    private String title;

    /**
     * 컨텐츠 설명
     */
    @Column(nullable = false, length = 2000)
    private String description;

    /**
     * 썸네일 이미지 URL
     */
    @Column(nullable = false)
    private String thumbnailUrl;

    /**
     * 평균 평점
     */
    @Column(nullable = false)
    private double averageRating;

    /**
     * 리뷰 개수
     */
    @Column(nullable = false)
    private int reviewCount;

    /**
     * 시청자 수
     */
    @Transient
    @Column(nullable = false)
    private long watcherCount;

    public void updateContent(String title, String description, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setWatcherCount(long watcherCount) {
        this.watcherCount = watcherCount;
    }

    /**
     * 컨텐츠 편의 메서드(리뷰용)
     */

    // 리뷰 생성 시 호출
    public void applyReviewCreated(int newRating) {
        int beforeCount = this.reviewCount;
        double beforeAvg = this.averageRating;

        int afterCount = beforeCount + 1;
        double afterAvg = ((beforeAvg * beforeCount) + newRating) / afterCount;

        this.reviewCount = afterCount;
        this.averageRating = afterAvg;
    }

    // 리뷰 수정 시 호출 (기존 평점 -> 새로운 평점)
    public void applyReviewUpdated(int oldRating, int newRating) {
        if (this.reviewCount <= 0) {
            this.reviewCount = 1;
            this.averageRating = newRating;
            return;
        }

        double sum = this.averageRating * this.reviewCount;
        double newSum = sum - oldRating + newRating;
        double afterAvg = newSum / this.reviewCount;

        this.averageRating = afterAvg;
    }

    public void applyReviewDeleted(int deletedRating) {
        int beforeCount = this.reviewCount;

        if (beforeCount <= 0) {
            this.reviewCount = 0;
            this.averageRating = 0;
            return;
        }

        int afterCount = beforeCount - 1;

        if (afterCount == 0) {
            this.reviewCount = 0;
            this.averageRating = 0.0;
        } else {
            double afterAvg =
                    ((this.averageRating * beforeCount) - deletedRating)
                            / afterCount;

            this.reviewCount = afterCount;
            this.averageRating = afterAvg;
        }
    }
}
