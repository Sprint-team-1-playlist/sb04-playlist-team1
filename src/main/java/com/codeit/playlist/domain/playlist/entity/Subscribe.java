package com.codeit.playlist.domain.playlist.entity;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscribes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscribe extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;
}
