package com.sgarsgaya.codeatlas.dto.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "snapshots")
public class SnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String snapshotName;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active;

    protected SnapshotJpaEntity() {}

    public SnapshotJpaEntity(String snapshotName, Instant createdAt, boolean active) {
        this.snapshotName = snapshotName;
        this.createdAt = createdAt;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getSnapshotName() { return snapshotName; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
