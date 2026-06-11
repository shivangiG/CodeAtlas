package com.sgarsgaya.codeatlas.core.freshness;

public enum FreshnessVerdict {

    /** Snapshot fingerprints match all relevant files. */
    FRESH,

    /** At least one relevant file is newer than the snapshot. */
    STALE_FOR_RELEVANT_FILES,

    /** No active snapshot exists. */
    NO_GRAPH
}
