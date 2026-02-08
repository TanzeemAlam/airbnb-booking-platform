package com.airbnb.common.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * BaseEntity - Abstract base class for all entities
 * Provides common audit fields: id, createdAt, updatedAt, version
 * 
 * Usage:
 *   All entities should extend this class
 *   Automatically manages creation and update timestamps
 *   Supports optimistic locking via @Version field
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    // TODO: Add any common methods if needed in future (e.g., prePersist, preUpdate hooks)
}
