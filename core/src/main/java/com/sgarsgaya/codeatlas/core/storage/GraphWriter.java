package com.sgarsgaya.codeatlas.core.storage;

import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.GraphNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class GraphWriter {

    private static final String INSERT_NODE = """
            INSERT INTO nodes (id, kind, fq_signature, fallback_key, file_path,
                               source_range, attributes_json, created_from_snapshot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String INSERT_EDGE = """
            INSERT INTO edges (id, src_node_id, dst_node_id, kind, confidence,
                               evidence_source, source_file, source_range, reason,
                               created_from_snapshot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String INSERT_FINGERPRINT = """
            INSERT OR REPLACE INTO file_fingerprints (file_path, sha256, snapshot_id)
            VALUES (?, ?, ?)""";

    private static final String INSERT_SNAPSHOT = """
            INSERT INTO snapshot_meta (id, built_at, schema_version, indexer_version,
                                       full_or_incremental, validated)
            VALUES (?, ?, ?, ?, ?, ?)""";

    private final Connection connection;

    public GraphWriter(GraphDatabase db) {
        this.connection = db.connection();
    }

    public void insertNode(GraphNode node) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_NODE)) {
            bindNode(ps, node);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert node " + node.id(), e);
        }
    }

    public void insertEdge(GraphEdge edge) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_EDGE)) {
            bindEdge(ps, edge);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert edge " + edge.id(), e);
        }
    }

    public void insertFingerprint(String filePath, String sha256, String snapshotId) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_FINGERPRINT)) {
            ps.setString(1, filePath);
            ps.setString(2, sha256);
            ps.setString(3, snapshotId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert fingerprint for " + filePath, e);
        }
    }

    public void insertSnapshotMeta(String id, String builtAt, String schemaVersion,
                                   String indexerVersion, String fullOrIncremental,
                                   boolean validated) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SNAPSHOT)) {
            ps.setString(1, id);
            ps.setString(2, builtAt);
            ps.setString(3, schemaVersion);
            ps.setString(4, indexerVersion);
            ps.setString(5, fullOrIncremental);
            ps.setString(6, String.valueOf(validated));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert snapshot meta " + id, e);
        }
    }

    public void bulkInsertNodes(List<GraphNode> nodes) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(INSERT_NODE)) {
                for (GraphNode node : nodes) {
                    bindNode(ps, node);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("Bulk node insert failed", e);
        } finally {
            restoreAutoCommit();
        }
    }

    public void bulkInsertEdges(List<GraphEdge> edges) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(INSERT_EDGE)) {
                for (GraphEdge edge : edges) {
                    bindEdge(ps, edge);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new IllegalStateException("Bulk edge insert failed", e);
        } finally {
            restoreAutoCommit();
        }
    }

    /**
     * Rebuilds the FTS5 indexes from the content tables.
     * Call after bulk inserts to keep full-text search up to date.
     */
    public void syncFts() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO nodes_fts(nodes_fts) VALUES('rebuild')");
            stmt.execute("INSERT INTO capabilities_fts(capabilities_fts) VALUES('rebuild')");
        } catch (SQLException e) {
            throw new IllegalStateException("FTS sync failed", e);
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────────

    private static void bindNode(PreparedStatement ps, GraphNode node) throws SQLException {
        ps.setString(1, node.id());
        ps.setString(2, node.kind().name());
        ps.setString(3, node.fqSignature());
        ps.setString(4, node.fallbackKey());
        ps.setString(5, node.filePath());
        ps.setString(6, node.sourceRange() == null ? null : node.sourceRange().toString());
        ps.setString(7, node.attributesJson());
        ps.setString(8, node.snapshotId());
    }

    private static void bindEdge(PreparedStatement ps, GraphEdge edge) throws SQLException {
        ps.setString(1, edge.id());
        ps.setString(2, edge.srcNodeId());
        ps.setString(3, edge.dstNodeId());
        ps.setString(4, edge.kind().name());
        ps.setString(5, edge.confidence().name());
        ps.setString(6, edge.evidenceSource().name());
        ps.setString(7, edge.sourceFile());
        ps.setString(8, edge.sourceRange() == null ? null : edge.sourceRange().toString());
        ps.setString(9, edge.reason());
        ps.setString(10, edge.snapshotId());
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // best-effort rollback
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // best-effort restore
        }
    }
}
