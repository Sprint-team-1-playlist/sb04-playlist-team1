package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.entity.Content;
import com.codeit.playlist.domain.content.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByContentId(UUID contentId);
    List<Tag> findByContentIdIn(List<UUID> contentIds);
    void deleteAllByContentId(UUID contentId);

    @Query("""
        select t.content.id as contentId, t.items as tag
        from Tag t
        where t.content.id in :contentIds
    """)
    List<TagProjection> findTagsRaw(@Param("contentIds") List<UUID> contentIds);

    interface TagProjection {
        UUID getContentId();
        String getTag();
    }

    default Map<UUID, List<String>> findTagsByContentIds(List<UUID> contentIds) {
        List<TagProjection> rows = findTagsRaw(contentIds);

        Map<UUID, List<String>> map = new HashMap<>();
        for (TagProjection row : rows) {
            map.computeIfAbsent(row.getContentId(), k -> new ArrayList<>())
                    .add(row.getTag());
        }

        return map;
    }

    List<Tag> findAllByContent(Content content);
}
