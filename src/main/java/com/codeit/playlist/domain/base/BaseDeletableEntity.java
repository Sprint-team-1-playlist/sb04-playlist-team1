package com.codeit.playlist.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public class BaseDeletableEntity extends BaseUpdatableEntity {

    @Column(name = "deletedAt")
    private LocalDateTime deletedAt;
}
