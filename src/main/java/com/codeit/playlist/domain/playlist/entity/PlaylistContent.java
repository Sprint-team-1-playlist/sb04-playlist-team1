package com.codeit.playlist.domain.playlist.entity;

import com.codeit.playlist.domain.base.BaseEntity;
import com.codeit.playlist.domain.content.entity.Content;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_contents")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistContent extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
}
