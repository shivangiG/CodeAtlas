package com.sgarsgaya.codeatlas.core.freshness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FreshnessCheckerTest {

    @TempDir
    Path tempDir;

    @Test
    void computeProducesConsistentSha256() throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello world");

        String first = FileFingerprint.compute(file);
        String second = FileFingerprint.compute(file);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
        assertThat(first).matches("[0-9a-f]{64}");
    }

    @Test
    void returnsNoGraphWhenNoPointerExists() {
        FreshnessChecker checker = new FreshnessChecker(tempDir);

        FreshnessResult result = checker.check(List.of("src/Main.java"));

        assertThat(result.verdict()).isEqualTo(FreshnessVerdict.NO_GRAPH);
        assertThat(result.staleFiles()).isEmpty();
        assertThat(result.activeSnapshotId()).isNull();
    }

    @Test
    void returnsFreshWhenFingerprintsMatch() throws Exception {
        Path srcFile = tempDir.resolve("src/Main.java");
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, "public class Main {}");

        String hash = FileFingerprint.compute(srcFile);
        String snapshotId = "snap-001";
        Path snapshotDb = setUpSnapshot(snapshotId);
        insertFingerprint(snapshotDb, "src/Main.java", hash, snapshotId);

        FreshnessChecker checker = new FreshnessChecker(tempDir);
        FreshnessResult result = checker.check(List.of("src/Main.java"));

        assertThat(result.verdict()).isEqualTo(FreshnessVerdict.FRESH);
        assertThat(result.staleFiles()).isEmpty();
        assertThat(result.activeSnapshotId()).isEqualTo(snapshotId);
    }

    @Test
    void returnsStaleWhenFileChanged() throws Exception {
        Path srcFile = tempDir.resolve("src/Main.java");
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, "public class Main {}");

        String snapshotId = "snap-002";
        Path snapshotDb = setUpSnapshot(snapshotId);
        insertFingerprint(snapshotDb, "src/Main.java", "0000000000000000000000000000000000000000000000000000000000000000", snapshotId);

        FreshnessChecker checker = new FreshnessChecker(tempDir);
        FreshnessResult result = checker.check(List.of("src/Main.java"));

        assertThat(result.verdict()).isEqualTo(FreshnessVerdict.STALE_FOR_RELEVANT_FILES);
        assertThat(result.staleFiles()).containsExactly("src/Main.java");
        assertThat(result.activeSnapshotId()).isEqualTo(snapshotId);
    }

    @Test
    void graphStatusFileRoundTrip() throws IOException {
        GraphStatusFile status = new GraphStatusFile();
        status.setActiveSnapshotId("snap-100");
        status.setFreshness("fresh");
        status.setRebuildWorkerActive(true);
        status.setLastBuildTime("2026-05-10T12:00:00Z");
        status.setLastBuildDuration("PT42S");
        status.setNodeCount(150);
        status.setEdgeCount(300);

        status.write(tempDir);

        GraphStatusFile loaded = GraphStatusFile.read(tempDir);
        assertThat(loaded.getActiveSnapshotId()).isEqualTo("snap-100");
        assertThat(loaded.getFreshness()).isEqualTo("fresh");
        assertThat(loaded.isRebuildWorkerActive()).isTrue();
        assertThat(loaded.getLastBuildTime()).isEqualTo("2026-05-10T12:00:00Z");
        assertThat(loaded.getLastBuildDuration()).isEqualTo("PT42S");
        assertThat(loaded.getNodeCount()).isEqualTo(150);
        assertThat(loaded.getEdgeCount()).isEqualTo(300);
    }

    @Test
    void graphStatusFileReturnsDefaultsWhenMissing() {
        GraphStatusFile loaded = GraphStatusFile.read(tempDir);

        assertThat(loaded.getFreshness()).isEqualTo("no_graph");
        assertThat(loaded.getActiveSnapshotId()).isNull();
        assertThat(loaded.isRebuildWorkerActive()).isFalse();
    }

    private Path setUpSnapshot(String snapshotId) throws IOException, SQLException {
        Path repoatlasDir = tempDir.resolve(".repoatlas");
        Files.createDirectories(repoatlasDir);

        Path snapshotsDir = repoatlasDir.resolve("snapshots");
        Files.createDirectories(snapshotsDir);

        Path snapshotDb = snapshotsDir.resolve(snapshotId);
        Files.writeString(repoatlasDir.resolve("graph_latest.pointer"), snapshotId + "\n");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + snapshotDb.toAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS file_fingerprints (
                        file_path   TEXT PRIMARY KEY,
                        sha256      TEXT NOT NULL,
                        snapshot_id TEXT NOT NULL
                    )""");
        }
        return snapshotDb;
    }

    private void insertFingerprint(Path snapshotDb, String filePath, String sha256, String snapshotId) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + snapshotDb.toAbsolutePath());
             var ps = conn.prepareStatement(
                     "INSERT INTO file_fingerprints (file_path, sha256, snapshot_id) VALUES (?, ?, ?)")) {
            ps.setString(1, filePath);
            ps.setString(2, sha256);
            ps.setString(3, snapshotId);
            ps.executeUpdate();
        }
    }
}
