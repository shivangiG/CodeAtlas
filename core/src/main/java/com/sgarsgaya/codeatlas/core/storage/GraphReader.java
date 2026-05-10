package com.sgarsgaya.codeatlas.core.storage;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.model.SourceRange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GraphReader {

    private final Connection connection;

    public GraphReader(GraphDatabase db) {
        this.connection = db.connection();
    }

    public Optional<GraphNode> findNodeById(String id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM nodes WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapNode(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find node by id " + id, e);
        }
    }

    public List<GraphNode> findNodesByKind(NodeKind kind) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM nodes WHERE kind = ?")) {
            ps.setString(1, kind.name());
            return collectNodes(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find nodes by kind " + kind, e);
        }
    }

    public List<GraphNode> findNodesByFile(String filePath) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM nodes WHERE file_path = ?")) {
            ps.setString(1, filePath);
            return collectNodes(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find nodes by file " + filePath, e);
        }
    }

    public Optional<GraphNode> findNodeBySignature(String fqSignature) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM nodes WHERE fq_signature = ?")) {
            ps.setString(1, fqSignature);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapNode(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find node by signature " + fqSignature, e);
        }
    }

    public List<GraphNode> searchNodes(String query) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT n.* FROM nodes n
                JOIN nodes_fts fts ON n.rowid = fts.rowid
                WHERE nodes_fts MATCH ?""")) {
            ps.setString(1, query);
            return collectNodes(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("FTS search failed for query: " + query, e);
        }
    }

    public List<GraphEdge> findEdgesFrom(String nodeId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM edges WHERE src_node_id = ?")) {
            ps.setString(1, nodeId);
            return collectEdges(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find edges from node " + nodeId, e);
        }
    }

    public List<GraphEdge> findEdgesTo(String nodeId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM edges WHERE dst_node_id = ?")) {
            ps.setString(1, nodeId);
            return collectEdges(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find edges to node " + nodeId, e);
        }
    }

    public List<GraphEdge> findEdgesByKind(EdgeKind kind) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM edges WHERE kind = ?")) {
            ps.setString(1, kind.name());
            return collectEdges(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find edges by kind " + kind, e);
        }
    }

    public Optional<String> getFingerprint(String filePath) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT sha256 FROM file_fingerprints WHERE file_path = ?")) {
            ps.setString(1, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("sha256")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get fingerprint for " + filePath, e);
        }
    }

    public Map<String, String> getAllFingerprints() {
        Map<String, String> result = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT file_path, sha256 FROM file_fingerprints")) {
            while (rs.next()) {
                result.put(rs.getString("file_path"), rs.getString("sha256"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load fingerprints", e);
        }
        return result;
    }

    public long countNodes() {
        return countTable("nodes");
    }

    public long countEdges() {
        return countTable("edges");
    }

    // ── private helpers ──────────────────────────────────────────────────────────

    private long countTable(String table) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count rows in " + table, e);
        }
    }

    private static List<GraphNode> collectNodes(PreparedStatement ps) throws SQLException {
        List<GraphNode> nodes = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nodes.add(mapNode(rs));
            }
        }
        return nodes;
    }

    private static List<GraphEdge> collectEdges(PreparedStatement ps) throws SQLException {
        List<GraphEdge> edges = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                edges.add(mapEdge(rs));
            }
        }
        return edges;
    }

    private static GraphNode mapNode(ResultSet rs) throws SQLException {
        String rangeStr = rs.getString("source_range");
        return new GraphNode(
                rs.getString("id"),
                NodeKind.valueOf(rs.getString("kind")),
                rs.getString("fq_signature"),
                rs.getString("fallback_key"),
                rs.getString("file_path"),
                rangeStr == null ? null : SourceRange.parse(rangeStr),
                rs.getString("attributes_json"),
                rs.getString("created_from_snapshot"));
    }

    private static GraphEdge mapEdge(ResultSet rs) throws SQLException {
        String rangeStr = rs.getString("source_range");
        return new GraphEdge(
                rs.getString("id"),
                rs.getString("src_node_id"),
                rs.getString("dst_node_id"),
                EdgeKind.valueOf(rs.getString("kind")),
                Confidence.valueOf(rs.getString("confidence")),
                EvidenceSource.valueOf(rs.getString("evidence_source")),
                rs.getString("source_file"),
                rangeStr == null ? null : SourceRange.parse(rangeStr),
                rs.getString("reason"),
                rs.getString("created_from_snapshot"));
    }
}
