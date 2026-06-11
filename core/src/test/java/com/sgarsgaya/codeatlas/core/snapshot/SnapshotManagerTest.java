package com.sgarsgaya.codeatlas.core.snapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotManagerTest {

    @TempDir
    Path repoRoot;

    private SnapshotManager manager;

    @BeforeEach
    void setUp() {
        manager = new SnapshotManager();
    }

    @Test
    void createNewSnapshot_createsFileAndReturnsPath() throws IOException {
        Path snapshot = manager.createNewSnapshot(repoRoot);

        assertThat(snapshot).exists();
        assertThat(snapshot.getFileName().toString()).isEqualTo("graph_S1.sqlite");
    }

    @Test
    void createNewSnapshot_sequentialIds() throws IOException {
        Path first = manager.createNewSnapshot(repoRoot);
        Path second = manager.createNewSnapshot(repoRoot);

        assertThat(first.getFileName().toString()).isEqualTo("graph_S1.sqlite");
        assertThat(second.getFileName().toString()).isEqualTo("graph_S2.sqlite");
    }

    @Test
    void nextSnapshotId_returnsS1ForEmptyDir() {
        assertThat(manager.nextSnapshotId(repoRoot)).isEqualTo("S1");
    }

    @Test
    void publish_validSnapshot_updatesPointerAndReturnsTrue() throws Exception {
        Path snapshot = manager.createNewSnapshot(repoRoot);
        populateValidSnapshot(snapshot);

        boolean result = manager.publish(repoRoot, snapshot);

        assertThat(result).isTrue();
        assertThat(manager.getActiveSnapshot(repoRoot)).contains(snapshot);
    }

    @Test
    void publish_invalidSnapshot_returnsFalse() throws IOException {
        Path snapshot = manager.createNewSnapshot(repoRoot);

        boolean result = manager.publish(repoRoot, snapshot);

        assertThat(result).isFalse();
        assertThat(manager.getActiveSnapshot(repoRoot)).isEmpty();
    }

    @Test
    void getActiveSnapshot_noPointer_returnsEmpty() {
        assertThat(manager.getActiveSnapshot(repoRoot)).isEmpty();
    }

    @Test
    void pointerRead_afterWrite_returnsSnapshotId() throws Exception {
        Path snapshot = manager.createNewSnapshot(repoRoot);
        String fileName = snapshot.getFileName().toString();

        SnapshotPointer.write(repoRoot, fileName);
        Optional<String> read = SnapshotPointer.read(repoRoot);

        assertThat(read).contains(fileName);
    }

    @Test
    void pointerRead_corruptPointer_returnsEmpty() throws Exception {
        Path dir = repoRoot.resolve(".repoatlas");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("graph_latest.pointer"), "nonexistent_file.sqlite\n");

        assertThat(SnapshotPointer.read(repoRoot)).isEmpty();
    }

    @Test
    void retentionPrune_keepsOnlyMostRecent() throws Exception {
        Path s1 = createValidPublishedSnapshot("S1");
        Path s2 = createValidPublishedSnapshot("S2");
        Path s3 = createValidPublishedSnapshot("S3");
        Path s4 = createValidPublishedSnapshot("S4");

        SnapshotPointer.write(repoRoot, s4.getFileName().toString());
        SnapshotRetention.prune(repoRoot, 2);

        assertThat(s1).doesNotExist();
        assertThat(s2).doesNotExist();
        assertThat(s3).exists();
        assertThat(s4).exists();
    }

    @Test
    void retentionPrune_neverDeletesActiveSnapshot() throws Exception {
        Path s1 = createValidPublishedSnapshot("S1");
        Path s2 = createValidPublishedSnapshot("S2");
        Path s3 = createValidPublishedSnapshot("S3");

        SnapshotPointer.write(repoRoot, s1.getFileName().toString());
        SnapshotRetention.prune(repoRoot, 1);

        assertThat(s1).exists();
        assertThat(s3).exists();
    }

    @Test
    void rebuildLock_exclusion() throws Exception {
        Optional<RebuildLock> first = RebuildLock.tryAcquire(repoRoot);
        assertThat(first).isPresent();

        Optional<RebuildLock> second = RebuildLock.tryAcquire(repoRoot);
        assertThat(second).isEmpty();

        first.get().close();

        Optional<RebuildLock> third = RebuildLock.tryAcquire(repoRoot);
        assertThat(third).isPresent();
        third.get().close();
    }

    @Test
    void validate_validSnapshot_returnsOk() throws Exception {
        Path snapshot = manager.createNewSnapshot(repoRoot);
        populateValidSnapshot(snapshot);

        ValidationResult result = SnapshotValidator.validate(snapshot);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validate_missingTables_returnsErrors() throws Exception {
        Path snapshot = manager.createNewSnapshot(repoRoot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE nodes (id TEXT PRIMARY KEY)");
        }

        ValidationResult result = SnapshotValidator.validate(snapshot);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Missing required table"));
    }

    @Test
    void validate_emptyNodes_returnsError() throws Exception {
        Path snapshot = manager.createNewSnapshot(repoRoot);
        createSchemaOnly(snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + snapshot);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO snapshot_meta (validated) VALUES ('true')");
        }

        ValidationResult result = SnapshotValidator.validate(snapshot);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("empty"));
    }

    private Path createValidPublishedSnapshot(String id) throws Exception {
        Path snapshotsDir = repoRoot.resolve(".repoatlas").resolve("snapshots");
        Files.createDirectories(snapshotsDir);
        Path path = snapshotsDir.resolve("graph_" + id + ".sqlite");
        Files.createFile(path);
        populateValidSnapshot(path);
        return path;
    }

    private void populateValidSnapshot(Path snapshot) throws SQLException {
        createSchemaOnly(snapshot);
        String jdbcUrl = "jdbc:sqlite:" + snapshot.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO nodes (id) VALUES ('node-1')");
            stmt.execute("INSERT INTO snapshot_meta (validated) VALUES ('true')");
        }
    }

    private void createSchemaOnly(Path snapshot) throws SQLException {
        String jdbcUrl = "jdbc:sqlite:" + snapshot.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS nodes (id TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS edges (id TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS capabilities (id TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS file_fingerprints (id TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS snapshot_meta (validated TEXT)");
        }
    }
}
