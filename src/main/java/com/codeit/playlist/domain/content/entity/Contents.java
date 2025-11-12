package com.codeit.playlist.domain.content.entity;

import com.codeit.playlist.domain.base.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contents extends BaseUpdatableEntity {

    /**
     * 컨텐츠 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Type type;

    /**
     * 컨텐츠 제목
     */
    @Column(length = 100, nullable = false)
    private String title;

    /**
     * 컨텐츠 설명
     */
    @Column(nullable = false)
    private String description;

    /**
     * 썸네일 이미지 URL
     */
    @Column(nullable = false)
    private String thumbnailUrl;

    /**
     * 컨텐츠 태그 목록
     */
    @Column(nullable = false)
    private String tags;

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
    @Column(nullable = false)
    private int watcherCount;
}
