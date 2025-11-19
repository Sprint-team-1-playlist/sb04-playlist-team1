package com.codeit.playlist.domain.playlist.entity;

import com.codeit.playlist.domain.base.BaseDeletableEntity;
import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Playlist extends BaseDeletableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "subscriber_count", nullable = false)
    private Long subscriberCount = 0L;

    @OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY,
                cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistContent> playlistContents = new ArrayList<>();

    // 편의 메서드
    public void addContent(Content content) {
        PlaylistContent pc = new PlaylistContent(this, content);
        playlistContents.add(pc);
    }

    public void removeContent(Content content) {
        playlistContents.removeIf(pc -> pc.getContent().equals(content));
    }

    public void updateInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
