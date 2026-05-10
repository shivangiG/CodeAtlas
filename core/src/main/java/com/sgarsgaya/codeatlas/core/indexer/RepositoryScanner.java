package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig;
import com.sgarsgaya.codeatlas.core.freshness.FileFingerprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Gradle-ish Java repository walker respecting ignore files + generated exclusions. */
public final class RepositoryScanner {

    private static final Logger log = LoggerFactory.getLogger(RepositoryScanner.class);

    private final ConcurrentHashMap<Path, List<PathMatcher>> ignoreMatchersCache = new ConcurrentHashMap<>();

    public ScanResult scan(Path repoRoot, RepoAtlasConfig config) throws IOException {
        RepoAtlasConfig resolved = Objects.requireNonNullElseGet(config, RepoAtlasConfig::new);
        Path root = repoRoot.toAbsolutePath().normalize();

        List<PathMatcher> generatedMatchers =
                matchersForGlobs(resolved.getGenerated().getPaths());
        IgnoreLineCompiler.PathMatcherContext ctx = IgnoreLineCompiler.PathMatcherContext.defaults();

        List<GradleModule> modules = discoverGradleModules(root, generatedMatchers, ctx);
        List<GradleModule> effectiveModules = modules.isEmpty() ? fallbackModule(root) : modules;

        LinkedHashMap<String, SourceFile> byRelPath = new LinkedHashMap<>();

        for (GradleModule module : effectiveModules) {
            for (Path sourceRoot : module.sourceRoots()) {
                if (!Files.isDirectory(sourceRoot)) {
                    continue;
                }

                try (var stream = Files.walk(sourceRoot)) {
                    stream.filter(Files::isRegularFile).forEach(javaFile -> {
                        if (!javaFile.getFileName().toString().endsWith(".java")) {
                            return;
                        }

                        if (isStructuralSkip(root, javaFile)) {
                            return;
                        }

                        Path parent = javaFile.getParent();
                        if (parent == null) {
                            return;
                        }

                        List<PathMatcher> effective =
                                matchersForAncestorDirs(root, parent, generatedMatchers, ctx);

                        boolean ignored = effective.stream()
                                .anyMatch(m -> IgnoreLineCompiler.matchesRepoRelative(root, javaFile, m));
                        if (ignored) {
                            log.debug(
                                    "Skipping ignored path: {}",
                                    IgnoreLineCompiler.toUnixPathString(root.relativize(javaFile)));
                            return;
                        }

                        Path relPath = root.relativize(javaFile).normalize();
                        String posix = posixKey(relPath);
                        boolean test = posix.contains("/src/test/java/");
                        try {
                            String fp = FileFingerprint.compute(javaFile);
                            byRelPath.put(
                                    posix,
                                    new SourceFile(javaFile, relPath, fp, module.name(), test));
                        } catch (IOException io) {
                            log.warn("Skipping java file (fingerprint failure) {}: {}", posix, io.getMessage());
                        }
                    });
                }
            }
        }

        Map<String, String> fingerprints = LinkedHashMap.newLinkedHashMap(byRelPath.size());
        List<SourceFile> sourceFiles = new ArrayList<>();
        byRelPath.forEach((posix, sf) -> {
            fingerprints.put(posix, sf.fingerprint());
            sourceFiles.add(sf);
        });

        return new ScanResult(root, effectiveModules, sourceFiles, fingerprints);
    }

    private boolean isStructuralSkip(Path repoRoot, Path candidate) {
        Path rel = repoRoot.relativize(candidate).normalize();
        String unix = posixKey(rel);
        return unix.startsWith(".git/")
                || unix.equals(".git")
                || unix.startsWith(".repoatlas/")
                || unix.equals(".repoatlas");
    }

    private List<GradleModule> fallbackModule(Path root) {
        List<Path> roots = new ArrayList<>();
        Path main = root.resolve("src/main/java");
        Path test = root.resolve("src/test/java");
        if (Files.isDirectory(main)) {
            roots.add(main);
        }
        if (Files.isDirectory(test)) {
            roots.add(test);
        }
        if (roots.isEmpty()) {
            return List.of();
        }
        return List.of(new GradleModule("root", root, roots));
    }

    private List<GradleModule> discoverGradleModules(
            Path root, List<PathMatcher> generatedMatchers, IgnoreLineCompiler.PathMatcherContext ctx) {
        List<GradleModule> modules = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                String name = file.getFileName().toString();
                if (!"build.gradle".equals(name) && !"build.gradle.kts".equals(name)) {
                    return;
                }

                if (isStructuralSkip(root, file)) {
                    return;
                }

                Path moduleDir = file.getParent();
                if (moduleDir == null) {
                    return;
                }

                if (moduleDirMatchesIgnore(root, moduleDir, generatedMatchers, ctx)) {
                    return;
                }

                String moduleName = moduleDir.getFileName().toString();
                List<Path> sourceRoots = new ArrayList<>();
                Path main = moduleDir.resolve("src/main/java");
                Path test = moduleDir.resolve("src/test/java");
                if (Files.isDirectory(main)) {
                    sourceRoots.add(main);
                }
                if (Files.isDirectory(test)) {
                    sourceRoots.add(test);
                }
                if (sourceRoots.isEmpty()) {
                    return;
                }

                modules.add(new GradleModule(moduleName, moduleDir, sourceRoots));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return modules;
    }

    private boolean moduleDirMatchesIgnore(
            Path root, Path moduleDir, List<PathMatcher> generatedMatchers, IgnoreLineCompiler.PathMatcherContext ctx) {
        List<PathMatcher> effective = matchersForAncestorDirs(root, moduleDir, generatedMatchers, ctx);
        return effective.stream().anyMatch(m -> IgnoreLineCompiler.matchesRepoRelative(root, moduleDir, m));
    }

    private List<PathMatcher> matchersForAncestorDirs(
            Path repoRoot,
            Path absoluteDir,
            List<PathMatcher> generatedMatchers,
            IgnoreLineCompiler.PathMatcherContext ctx) {

        Path key = absoluteDir.toAbsolutePath().normalize();
        return ignoreMatchersCache.computeIfAbsent(key, dir -> {
            List<PathMatcher> acc = new ArrayList<>(generatedMatchers);

            Path cursor = repoRoot.normalize();
            acc.addAll(readIgnoreLines(cursor, ctx));

            Path rel = repoRoot.relativize(dir).normalize();
            for (int i = 0; i < rel.getNameCount(); i++) {
                cursor = cursor.resolve(rel.getName(i)).normalize();
                acc.addAll(readIgnoreLines(cursor, ctx));
            }

            return List.copyOf(acc);
        });
    }

    private static List<PathMatcher> readIgnoreLines(Path dir, IgnoreLineCompiler.PathMatcherContext ctx) {
        List<PathMatcher> matchers = new ArrayList<>();
        appendIgnoreFile(dir.resolve(".gitignore"), ctx, matchers);
        appendIgnoreFile(dir.resolve(".repoatlasignore"), ctx, matchers);
        return matchers;
    }

    private static void appendIgnoreFile(Path file, IgnoreLineCompiler.PathMatcherContext ctx, List<PathMatcher> out) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                IgnoreLineCompiler.compile(line, ctx).ifPresent(out::add);
            }
        } catch (IOException e) {
            log.warn("Failed reading ignore file {}: {}", file, e.getMessage());
        }
    }

    private static List<PathMatcher> matchersForGlobs(List<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String glob = pattern.startsWith("glob:") ? pattern : "glob:" + pattern;
            matchers.add(FileSystems.getDefault().getPathMatcher(glob));
        }
        return matchers;
    }

    private static String posixKey(Path repoRelative) {
        StringBuilder sb = new StringBuilder();
        for (Path part : repoRelative) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(part.toString());
        }
        return sb.toString();
    }
}
