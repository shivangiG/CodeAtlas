package com.sgarsgaya.codeatlas.core.freshness;

import com.sgarsgaya.codeatlas.core.snapshot.SnapshotPointer;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class FreshnessChecker {

    private static final Logger LOG = Logger.getLogger(FreshnessChecker.class.getName());

    private final Path repoRoot;

    public FreshnessChecker(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public FreshnessResult check(List<String> relevantFiles) {
        Optional<String> snapshotId = SnapshotPointer.read(repoRoot);
        if (snapshotId.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.NO_GRAPH, List.of(), null);
        }

        String id = snapshotId.get();
        Optional<Path> snapshotPath = SnapshotPointer.resolve(repoRoot);
        if (snapshotPath.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.NO_GRAPH, List.of(), null);
        }

        Map<String, String> storedFingerprints = readFingerprints(snapshotPath.get(), relevantFiles);
        List<String> staleFiles = findStaleFiles(relevantFiles, storedFingerprints);

        if (staleFiles.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.FRESH, List.of(), id);
        }
        return new FreshnessResult(FreshnessVerdict.STALE_FOR_RELEVANT_FILES, staleFiles, id);
    }

    public FreshnessResult checkAll() {
        Optional<String> snapshotId = SnapshotPointer.read(repoRoot);
        if (snapshotId.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.NO_GRAPH, List.of(), null);
        }

        String id = snapshotId.get();
        Optional<Path> snapshotPath = SnapshotPointer.resolve(repoRoot);
        if (snapshotPath.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.NO_GRAPH, List.of(), null);
        }

        Map<String, String> allFingerprints = readAllFingerprints(snapshotPath.get());
        List<String> allFiles = new ArrayList<>(allFingerprints.keySet());
        List<String> staleFiles = findStaleFiles(allFiles, allFingerprints);

        if (staleFiles.isEmpty()) {
            return new FreshnessResult(FreshnessVerdict.FRESH, List.of(), id);
        }
        return new FreshnessResult(FreshnessVerdict.STALE_FOR_RELEVANT_FILES, staleFiles, id);
    }

    private List<String> findStaleFiles(List<String> filePaths, Map<String, String> storedFingerprints) {
        List<String> stale = new ArrayList<>();
        for (String filePath : filePaths) {
            String storedHash = storedFingerprints.get(filePath);
            if (storedHash == null) {
                stale.add(filePath);
                continue;
            }
            try {
                Path resolved = repoRoot.resolve(filePath);
                String currentHash = FileFingerprint.compute(resolved);
                if (!currentHash.equals(storedHash)) {
                    stale.add(filePath);
                }
            } catch (IOException e) {
                LOG.warning("Cannot compute fingerprint for " + filePath + ": " + e.getMessage());
                stale.add(filePath);
            }
        }
        return List.copyOf(stale);
    }

    private Map<String, String> readFingerprints(Path snapshotDb, List<String> filePaths) {
        Map<String, String> result = new HashMap<>();
        String url = "jdbc:sqlite:" + snapshotDb.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT file_path, sha256 FROM file_fingerprints WHERE file_path = ?")) {
                for (String path : filePaths) {
                    ps.setString(1, path);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            result.put(rs.getString("file_path"), rs.getString("sha256"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("Failed to read fingerprints from snapshot: " + e.getMessage());
        }
        return result;
    }

    private Map<String, String> readAllFingerprints(Path snapshotDb) {
        Map<String, String> result = new HashMap<>();
        String url = "jdbc:sqlite:" + snapshotDb.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT file_path, sha256 FROM file_fingerprints");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("file_path"), rs.getString("sha256"));
            }
        } catch (SQLException e) {
            LOG.warning("Failed to read all fingerprints from snapshot: " + e.getMessage());
        }
        return result;
    }
}
