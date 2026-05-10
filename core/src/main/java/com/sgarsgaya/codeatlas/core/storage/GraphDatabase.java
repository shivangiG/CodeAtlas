package com.sgarsgaya.codeatlas.core.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class GraphDatabase implements AutoCloseable {

    private final Connection connection;

    public GraphDatabase(Path sqlitePath) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath.toAbsolutePath());
            configurePragmas();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not found on classpath", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open SQLite database at " + sqlitePath, e);
        }
    }

    private void configurePragmas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }
    }

    public void initSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nodes (
                        id                    TEXT PRIMARY KEY,
                        kind                  TEXT NOT NULL,
                        fq_signature          TEXT,
                        fallback_key          TEXT,
                        file_path             TEXT,
                        source_range          TEXT,
                        attributes_json       TEXT,
                        created_from_snapshot TEXT NOT NULL
                    )""");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS edges (
                        id                    TEXT PRIMARY KEY,
                        src_node_id           TEXT NOT NULL REFERENCES nodes(id),
                        dst_node_id           TEXT NOT NULL REFERENCES nodes(id),
                        kind                  TEXT NOT NULL,
                        confidence            TEXT NOT NULL,
                        evidence_source       TEXT NOT NULL,
                        source_file           TEXT NOT NULL,
                        source_range          TEXT NOT NULL,
                        reason                TEXT NOT NULL,
                        created_from_snapshot TEXT NOT NULL
                    )""");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS capabilities (
                        id               TEXT PRIMARY KEY,
                        service          TEXT NOT NULL,
                        display_name     TEXT NOT NULL,
                        domain           TEXT NOT NULL,
                        client_method    TEXT,
                        transport_json   TEXT,
                        business_outcome TEXT,
                        side_effects_json TEXT,
                        reuse_priority   TEXT,
                        manifest_source  TEXT,
                        bundle_version   TEXT
                    )""");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS file_fingerprints (
                        file_path   TEXT PRIMARY KEY,
                        sha256      TEXT NOT NULL,
                        snapshot_id TEXT NOT NULL
                    )""");

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS snapshot_meta (
                        id                  TEXT PRIMARY KEY,
                        built_at            TEXT NOT NULL,
                        schema_version      TEXT NOT NULL,
                        indexer_version     TEXT NOT NULL,
                        full_or_incremental TEXT NOT NULL,
                        validated           TEXT NOT NULL
                    )""");

            // FTS5 virtual tables
            stmt.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS nodes_fts
                    USING fts5(id, fq_signature, file_path,
                               content=nodes, content_rowid=rowid)""");

            stmt.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS capabilities_fts
                    USING fts5(id, display_name, business_outcome,
                               content=capabilities, content_rowid=rowid)""");

            // Indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_kind         ON nodes(kind)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_file_path    ON nodes(file_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_fq_signature ON nodes(fq_signature)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_src          ON edges(src_node_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_dst          ON edges(dst_node_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_kind         ON edges(kind)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise graph schema", e);
        }
    }

    Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close SQLite connection", e);
        }
    }
}
