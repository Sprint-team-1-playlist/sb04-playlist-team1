package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {
    boolean existsByApiId(Long apiId);
    boolean existsByTypeAndApiId(String type, Long apiId);
}
