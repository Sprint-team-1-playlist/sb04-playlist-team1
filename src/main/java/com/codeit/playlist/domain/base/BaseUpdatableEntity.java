package com.codeit.playlist.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@EntityListeners(AuditingEntityListener.class)  // Auditing
@MappedSuperclass
public class BaseUpdatableEntity extends BaseEntity {

    @LastModifiedDate
    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;
}
