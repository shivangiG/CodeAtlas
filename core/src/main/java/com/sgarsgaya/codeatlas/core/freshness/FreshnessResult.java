package com.sgarsgaya.codeatlas.core.freshness;

import java.util.List;

public record FreshnessResult(
    FreshnessVerdict verdict,
    List<String> staleFiles,
    String activeSnapshotId
) {}
