package com.codeit.playlist.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public class BaseDeletableEntity extends BaseUpdatableEntity {

    @Column(name = "deletedAt")
    private Instant deletedAt;

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
