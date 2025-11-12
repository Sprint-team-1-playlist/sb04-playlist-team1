package com.codeit.playlist.domain.content.entity;

import com.codeit.playlist.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Tags extends BaseEntity {

    /**
     * 콘텐츠 ID
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contents", columnDefinition = "uuid")
    private Contents contentsId;

    /**
     * 태그명
     */
    @Column(nullable = false, length = 50)
    private String items;

    public Tags(String items) {
        this.items = items;
    }
}
