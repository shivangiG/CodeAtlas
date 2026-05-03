package com.sgarsgaya.codeatlas.repositories;

import java.util.Optional;

import com.sgarsgaya.codeatlas.dto.entity.SnapshotJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SnapshotJpaRepository extends JpaRepository<SnapshotJpaEntity, Long> {

    Optional<SnapshotJpaEntity> findBySnapshotName(String snapshotName);

    Optional<SnapshotJpaEntity> findByActiveTrue();

    void deleteBySnapshotName(String snapshotName);

    @Modifying
    @Query("UPDATE SnapshotJpaEntity s SET s.active = false")
    void deactivateAll();
}
