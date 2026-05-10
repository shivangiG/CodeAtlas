package com.sgarsgaya.codeatlas.core.indexer;

import java.nio.file.Path;
import java.util.List;

/**
 * A Gradle-ish module inferred from presence of {@code build.gradle(.kts)} and standard source dirs.
 */
public record GradleModule(String name, Path path, List<Path> sourceRoots) {}
