package com.sgarsgaya.codeatlas.core.snapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.logging.Logger;

public final class SnapshotPointer {

    private static final Logger LOG = Logger.getLogger(SnapshotPointer.class.getName());
    private static final String POINTER_FILE = "graph_latest.pointer";
    private static final String REPOATLAS_DIR = ".repoatlas";
    private static final String SNAPSHOTS_DIR = "snapshots";

    private SnapshotPointer() {}

    public static Optional<String> read(Path repoRoot) {
        Path pointerFile = repoRoot.resolve(REPOATLAS_DIR).resolve(POINTER_FILE);
        if (!Files.exists(pointerFile)) {
            return Optional.empty();
        }
        try {
            String snapshotId = Files.readString(pointerFile).strip();
            if (snapshotId.isEmpty()) {
                return Optional.empty();
            }

            Path snapshotPath = repoRoot.resolve(REPOATLAS_DIR).resolve(SNAPSHOTS_DIR)
                    .resolve(snapshotId);
            if (!Files.exists(snapshotPath)) {
                LOG.warning("Pointer references nonexistent snapshot: " + snapshotId);
                return Optional.empty();
            }
            return Optional.of(snapshotId);
        } catch (IOException e) {
            LOG.warning("Failed to read pointer file: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void write(Path repoRoot, String snapshotId) throws IOException {
        Path dir = repoRoot.resolve(REPOATLAS_DIR);
        Files.createDirectories(dir);

        Path pointerFile = dir.resolve(POINTER_FILE);
        Path tmpFile = dir.resolve(POINTER_FILE + ".tmp");

        Files.writeString(tmpFile, snapshotId + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try (FileChannel channel = FileChannel.open(tmpFile, StandardOpenOption.WRITE)) {
            channel.force(true);
        }

        Files.move(tmpFile, pointerFile,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Optional<Path> resolve(Path repoRoot) {
        return read(repoRoot).map(id ->
                repoRoot.resolve(REPOATLAS_DIR).resolve(SNAPSHOTS_DIR).resolve(id));
    }
}
