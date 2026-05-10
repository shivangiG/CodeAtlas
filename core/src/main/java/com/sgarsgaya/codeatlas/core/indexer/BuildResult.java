package com.sgarsgaya.codeatlas.core.indexer;

import java.util.List;

/** Outcome summary for RepoAtlas indexer pipeline runs (design §9 orchestration). */
public record BuildResult(
        boolean success,
        String snapshotId,
        String snapshotFileName,
        int nodeCount,
        int edgeCount,
        int fileCount,
        long durationMs,
        List<String> diagnostics,
        List<String> ambiguities) {

    public static BuildResult failure(String message) {
        return new BuildResult(
                false, null, null, 0, 0, 0, 0, List.of(message), List.of());
    }
}
