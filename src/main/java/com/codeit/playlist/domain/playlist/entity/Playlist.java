package com.codeit.playlist.domain.playlist.entity;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.beans.ConstructorProperties;

@Entity
@Table(name = "play_lists")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Playlist extends BaseEntity {

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true) //임시로 nullable 허용
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "subscriber_count", nullable = false)
    private Long subscriberCount = 0L;

    @ConstructorProperties({"owner","title","description"})
    public Playlist(User owner, String title, String description) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.subscriberCount = 0L;
    }

    public Playlist(String title, String description) {
        this(null, title, description);
    }
}
