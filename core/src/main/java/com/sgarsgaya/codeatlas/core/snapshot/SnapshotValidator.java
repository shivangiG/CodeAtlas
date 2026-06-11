package com.sgarsgaya.codeatlas.core.snapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SnapshotValidator {

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "nodes", "edges", "capabilities", "file_fingerprints", "snapshot_meta");

    private SnapshotValidator() {}

    public static ValidationResult validate(Path snapshotPath) {
        List<String> errors = new ArrayList<>();

        if (!Files.exists(snapshotPath)) {
            return ValidationResult.failure(List.of("Snapshot file does not exist: " + snapshotPath));
        }

        String jdbcUrl = "jdbc:sqlite:" + snapshotPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            checkRequiredTables(stmt, errors);
            if (!errors.isEmpty()) {
                return ValidationResult.failure(errors);
            }

            checkForeignKeys(stmt, errors);
            checkSnapshotMeta(stmt, errors);
            checkNodeCount(stmt, errors);

        } catch (SQLException e) {
            errors.add("Failed to open SQLite database: " + e.getMessage());
        }

        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        return ValidationResult.failure(errors);
    }

    private static void checkRequiredTables(Statement stmt, List<String> errors) throws SQLException {
        ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table'");
        Set<String> existingTables = new java.util.HashSet<>();
        while (rs.next()) {
            existingTables.add(rs.getString("name"));
        }
        for (String required : REQUIRED_TABLES) {
            if (!existingTables.contains(required)) {
                errors.add("Missing required table: " + required);
            }
        }
    }

    private static void checkForeignKeys(Statement stmt, List<String> errors) throws SQLException {
        ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check");
        int violations = 0;
        while (rs.next()) {
            violations++;
        }
        if (violations > 0) {
            errors.add("Foreign key violations found: " + violations);
        }
    }

    private static void checkSnapshotMeta(Statement stmt, List<String> errors) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM snapshot_meta");
        if (rs.next() && rs.getInt("cnt") != 1) {
            errors.add("snapshot_meta must have exactly one row, found: " + rs.getInt("cnt"));
            return;
        }

        rs = stmt.executeQuery("SELECT validated FROM snapshot_meta");
        if (rs.next()) {
            String validated = rs.getString("validated");
            if (!"true".equals(validated)) {
                errors.add("snapshot_meta.validated is not 'true', found: " + validated);
            }
        }
    }

    private static void checkNodeCount(Statement stmt, List<String> errors) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM nodes");
        if (rs.next() && rs.getInt("cnt") == 0) {
            errors.add("Node table is empty — a valid graph must have at least one node");
        }
    }
}
