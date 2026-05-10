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

public final class SnapshotRetention {

    private static final Logger LOG = Logger.getLogger(SnapshotRetention.class.getName());
    private static final String REPOATLAS_DIR = ".repoatlas";
    private static final String SNAPSHOTS_DIR = "snapshots";
    public static final int DEFAULT_KEEP_COUNT = 3;

    private SnapshotRetention() {}

    public static void prune(Path repoRoot, int keepCount) {
        Path snapshotsDir = repoRoot.resolve(REPOATLAS_DIR).resolve(SNAPSHOTS_DIR);
        if (!Files.isDirectory(snapshotsDir)) {
            return;
        }

        Optional<String> activeId = SnapshotPointer.read(repoRoot);

        List<String> snapshots = listSnapshots(snapshotsDir);
        Collections.sort(snapshots);

        if (snapshots.size() <= keepCount) {
            return;
        }

        List<String> toDelete = snapshots.subList(0, snapshots.size() - keepCount);
        for (String name : toDelete) {
            if (activeId.isPresent() && activeId.get().equals(name)) {
                continue;
            }
            try {
                Files.deleteIfExists(snapshotsDir.resolve(name));
                LOG.info("Pruned old snapshot: " + name);
            } catch (IOException e) {
                LOG.warning("Failed to delete snapshot " + name + ": " + e.getMessage());
            }
        }
    }

    private static List<String> listSnapshots(Path snapshotsDir) {
        List<String> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotsDir, "graph_S*.sqlite")) {
            for (Path entry : stream) {
                result.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            LOG.warning("Failed to list snapshots: " + e.getMessage());
        }
        return result;
    }
}
