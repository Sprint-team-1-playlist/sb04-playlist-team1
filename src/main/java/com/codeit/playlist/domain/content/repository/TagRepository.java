package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByContentId(UUID contentId);
    List<Tag> findByContentIdIn(List<UUID> contentIds);
    void deleteAllByContentId(UUID contentId);

    List<Tag> findAllByContent(Content content);
}
