package com.sgarsgaya.codeatlas.core.snapshot;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public final class SnapshotManager {

    private static final Logger LOG = Logger.getLogger(SnapshotManager.class.getName());
    private static final String REPOATLAS_DIR = ".repoatlas";
    private static final String SNAPSHOTS_DIR = "snapshots";

    public Path createNewSnapshot(Path repoRoot) throws IOException {
        Path snapshotsDir = repoRoot.resolve(REPOATLAS_DIR).resolve(SNAPSHOTS_DIR);
        Files.createDirectories(snapshotsDir);

        String nextId = nextSnapshotId(repoRoot);
        Path snapshotPath = snapshotsDir.resolve("graph_" + nextId + ".sqlite");
        Files.createFile(snapshotPath);
        return snapshotPath;
    }

    public boolean publish(Path repoRoot, Path snapshotPath) {
        try (RebuildLock lock = RebuildLock.tryAcquire(repoRoot).orElse(null)) {
            if (lock == null) {
                LOG.warning("Cannot publish: rebuild lock is held by another process");
                return false;
            }

            ValidationResult result = SnapshotValidator.validate(snapshotPath);
            if (!result.valid()) {
                LOG.warning("Snapshot validation failed: " + result.errors());
                return false;
            }

            String snapshotFileName = snapshotPath.getFileName().toString();
            SnapshotPointer.write(repoRoot, snapshotFileName);
            SnapshotRetention.prune(repoRoot, SnapshotRetention.DEFAULT_KEEP_COUNT);
            return true;
        } catch (IOException e) {
            LOG.warning("Failed to publish snapshot: " + e.getMessage());
            return false;
        }
    }

    public Optional<Path> getActiveSnapshot(Path repoRoot) {
        return SnapshotPointer.resolve(repoRoot);
    }

    public String nextSnapshotId(Path repoRoot) {
        Path snapshotsDir = repoRoot.resolve(REPOATLAS_DIR).resolve(SNAPSHOTS_DIR);
        if (!Files.isDirectory(snapshotsDir)) {
            return "S1";
        }

        List<Integer> indices = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotsDir, "graph_S*.sqlite")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                String numPart = name.replace("graph_S", "").replace(".sqlite", "");
                try {
                    indices.add(Integer.parseInt(numPart));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to scan snapshots directory: " + e.getMessage());
        }

        if (indices.isEmpty()) {
            return "S1";
        }
        Collections.sort(indices);
        return "S" + (indices.get(indices.size() - 1) + 1);
    }
}
