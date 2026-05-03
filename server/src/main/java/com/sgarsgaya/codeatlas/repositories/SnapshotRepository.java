package com.sgarsgaya.codeatlas.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;

/**
 * Domain-facing persistence seam for snapshots.
 * All methods return {@link SnapshotResponseDto} — no JPA types cross this interface.
 * Mapping from {@code SnapshotJpaEntity} is handled by {@code SnapshotMapper} inside the adapter.
 */
public interface SnapshotRepository {

    SnapshotResponseDto save(String snapshotName, Instant createdAt);

    List<SnapshotResponseDto> findAll();

    Optional<SnapshotResponseDto> findActive();

    Optional<SnapshotResponseDto> findByName(String snapshotName);

    void deleteByName(String snapshotName);
}
