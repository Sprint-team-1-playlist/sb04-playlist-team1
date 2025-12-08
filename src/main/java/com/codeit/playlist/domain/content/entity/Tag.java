package com.codeit.playlist.domain.content.entity;

import com.codeit.playlist.domain.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Tag extends BaseEntity {
    /**
     * 콘텐츠 ID
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contents", columnDefinition = "uuid")
    private Content content;

    /**
     * API호출했을 때 들어오는 콘텐츠 데이터의 ID
     */
//    @Column(name = "movie_id", nullable = false)
//    private Long movieId;

    /**
     * 태그명
     */
    @Column(nullable = false, length = 50)
    private String items;

    public static Tag withContent(Content content, String items) {
        return new Tag(content, items);
    }
}
