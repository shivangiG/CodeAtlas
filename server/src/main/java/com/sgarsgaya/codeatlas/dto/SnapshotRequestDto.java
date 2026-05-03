package com.sgarsgaya.codeatlas.dto;

import jakarta.validation.constraints.NotBlank;

public record SnapshotRequestDto(
        @NotBlank(message = "snapshotName is required")
        String snapshotName
) {
}
