package com.sgarsgaya.codeatlas.dto;

public record GraphStatusResponseDto(
        String snapshotId,
        String graphStatus,
        String createdAt,
        Boolean rebuildWorkerActive
) {}
