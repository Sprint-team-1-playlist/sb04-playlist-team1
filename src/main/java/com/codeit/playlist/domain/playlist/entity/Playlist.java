package com.codeit.playlist.domain.playlist.entity;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "play_lists")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playlist extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "subscriber_count", nullable = false)
    @Builder.Default
    private Long subscriberCount = 0L;

}
