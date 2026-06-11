package com.sgarsgaya.codeatlas.core.indexer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Aggregate view of reachable Java sources plus fingerprints. */
public record ScanResult(
        Path repoRoot,
        List<GradleModule> modules,
        List<SourceFile> sourceFiles,
        Map<String, String> fingerprints) {}
