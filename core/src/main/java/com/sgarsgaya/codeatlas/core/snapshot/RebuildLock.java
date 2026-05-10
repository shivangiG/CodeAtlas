package com.sgarsgaya.codeatlas.core.snapshot;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.logging.Logger;

public final class RebuildLock implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(RebuildLock.class.getName());
    private static final String REPOATLAS_DIR = ".repoatlas";
    private static final String LOCK_FILE = "rebuild.lock";

    private final FileChannel channel;
    private final FileLock lock;

    private RebuildLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static Optional<RebuildLock> tryAcquire(Path repoRoot) {
        Path dir = repoRoot.resolve(REPOATLAS_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.warning("Cannot create .repoatlas directory: " + e.getMessage());
            return Optional.empty();
        }

        Path lockFile = dir.resolve(LOCK_FILE);
        try {
            FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock fileLock = channel.tryLock();
            if (fileLock == null) {
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new RebuildLock(channel, fileLock));
        } catch (OverlappingFileLockException e) {
            return Optional.empty();
        } catch (IOException e) {
            LOG.warning("Failed to acquire rebuild lock: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void release() {
        try {
            if (lock.isValid()) {
                lock.release();
            }
        } catch (IOException e) {
            LOG.warning("Failed to release file lock: " + e.getMessage());
        }
        try {
            channel.close();
        } catch (IOException e) {
            LOG.warning("Failed to close lock channel: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        release();
    }
}
