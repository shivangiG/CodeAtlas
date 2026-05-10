package com.sgarsgaya.codeatlas.core.indexer;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight gitignore-ish line → {@link PathMatcher}.
 *
 * <p>MVP semantics: ignores negations ({@code !}) and escapes; supports {@code **} as in config globs.</p>
 */
final class IgnoreLineCompiler {

    private IgnoreLineCompiler() {}

    /** Returns {@link PathMatcher} against paths relative to the repository root using forward slashes. */
    static Optional<PathMatcher> compile(String rawLine, PathMatcherContext ctx) {
        String line = Objects.requireNonNull(rawLine).strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return Optional.empty();
        }
        if (line.startsWith("!")) {
            return Optional.empty();
        }
        boolean trailingSlashDirectory = line.endsWith("/");
        if (trailingSlashDirectory) {
            line = line.substring(0, line.length() - 1);
        }

        boolean anchoredToRoot = line.startsWith("/");
        if (anchoredToRoot) {
            line = line.substring(1);
        }

        if (line.isEmpty()) {
            return Optional.empty();
        }

        String glob = anchoredToRoot ? anchoredGlob(line, trailingSlashDirectory)
                : unanchoredGlob(line, trailingSlashDirectory);

        return Optional.of(ctx.fileSystem().getPathMatcher("glob:" + glob));
    }

    /** Match {@link Path} relativized against {@code repoRoot} with POSIX-style separators for matching. */
    static boolean matchesRepoRelative(Path repoRoot, Path candidate, PathMatcher matcher) {
        Path rel = repoRoot.relativize(candidate).normalize();
        Path unixStyle = Paths.get(toUnixPathString(rel));
        return matcher.matches(unixStyle);
    }

    private static String anchoredGlob(String pattern, boolean directoryOnly) {
        if (directoryOnly) {
            return pattern + "/**";
        }
        return pattern.startsWith("**") ? pattern : pattern;
    }

    /**
     * Unanchored: match anywhere beneath the repo. If directory-only {@code trailingSlashDirectory},
     * only matches paths under that directory.
     */
    private static String unanchoredGlob(String pattern, boolean directoryOnly) {
        if (directoryOnly && !pattern.endsWith("/")) {
            return "**/" + pattern + "/**";
        }
        return "**/" + pattern;
    }

    static String toUnixPathString(Path repoRelativeNormalized) {
        StringBuilder sb = new StringBuilder();
        for (Path part : repoRelativeNormalized) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(part.toString());
        }
        return sb.toString();
    }

    /** FS handle for matchers; default system FS. */
    record PathMatcherContext(java.nio.file.FileSystem fileSystem) {
        PathMatcherContext {
            Objects.requireNonNull(fileSystem);
        }

        static PathMatcherContext defaults() {
            return new PathMatcherContext(FileSystems.getDefault());
        }
    }
}
