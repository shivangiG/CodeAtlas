package com.sgarsgaya.codeatlas.core.indexer;

import java.nio.file.Path;

/** One Java compilation unit tracked by RepoAtlas scanners. */
public record SourceFile(
        Path absolutePath,
        Path relativePath,
        String fingerprint,
        String moduleName,
        boolean testSource) {}
