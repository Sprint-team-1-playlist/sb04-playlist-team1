package com.codeit.playlist.domain.content.repository;

import com.codeit.playlist.domain.content.entity.Contents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentRepository extends JpaRepository<Contents, UUID> {
}
