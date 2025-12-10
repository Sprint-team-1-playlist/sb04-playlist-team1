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
     * The Movie API를 호출했을 때 들어오는 id
     */
    @Column(name = "genre_id", nullable = false)
    private Integer genreId;

    /**
     * 태그명("액션", "모험", "애니메이션", "코미디", ...)
     */
    @Column(nullable = false, length = 50)
    private String name;

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tag(Content content, String name) {
        this.content = content;
        this.name = name;
        this.genreId = 0;
    }
}
