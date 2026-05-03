package com.sgarsgaya.codeatlas.service;

import java.util.List;
import java.util.Optional;

import com.sgarsgaya.codeatlas.dto.GraphStatusResponseDto;
import com.sgarsgaya.codeatlas.dto.SnapshotRequestDto;
import com.sgarsgaya.codeatlas.dto.SnapshotResponseDto;

public interface SnapshotService {

    SnapshotResponseDto createSnapshot(SnapshotRequestDto request);

    List<SnapshotResponseDto> listSnapshots();

    Optional<GraphStatusResponseDto> getActiveSnapshot();

    /** @throws com.sgarsgaya.codeatlas.exception.SnapshotNotFoundException if not found (404) */
    /** @throws com.sgarsgaya.codeatlas.exception.ActiveSnapshotDeletionException if active (409) */
    void deleteSnapshot(String snapshotId);
}
