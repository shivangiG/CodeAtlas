package com.sgarsgaya.codeatlas.core.storage;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.model.SourceRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GraphDatabaseTest {

    private Path dbPath;
    private GraphDatabase db;
    private GraphWriter writer;
    private GraphReader reader;

    @BeforeEach
    void setUp() throws IOException {
        dbPath = Files.createTempFile("codeatlas-test-", ".db");
        db = new GraphDatabase(dbPath);
        db.initSchema();
        writer = new GraphWriter(db);
        reader = new GraphReader(db);
    }

    @AfterEach
    void tearDown() throws IOException {
        db.close();
        Files.deleteIfExists(dbPath);
    }

    @Test
    void schemaCreatesWithoutError() {
        // initSchema ran in setUp — a second call should be idempotent
        db.initSchema();
        assertThat(reader.countNodes()).isZero();
        assertThat(reader.countEdges()).isZero();
    }

    @Test
    void insertAndReadNode() {
        GraphNode node = new GraphNode(
                "n1", NodeKind.CLASS,
                "com.example.Foo", null,
                "src/main/java/com/example/Foo.java",
                new SourceRange(1, 0, 42, 1),
                "{\"abstract\":false}", "snap-001");

        writer.insertNode(node);

        Optional<GraphNode> found = reader.findNodeById("n1");
        assertThat(found).isPresent();
        assertThat(found.get().kind()).isEqualTo(NodeKind.CLASS);
        assertThat(found.get().fqSignature()).isEqualTo("com.example.Foo");
        assertThat(found.get().sourceRange()).isEqualTo(new SourceRange(1, 0, 42, 1));
        assertThat(found.get().snapshotId()).isEqualTo("snap-001");
    }

    @Test
    void insertAndReadEdge() {
        insertTwoNodes();

        GraphEdge edge = new GraphEdge(
                "e1", "n1", "n2", EdgeKind.CALLS,
                Confidence.HIGH, EvidenceSource.SOLVER,
                "src/main/java/com/example/Foo.java",
                new SourceRange(10, 4, 10, 30),
                "direct method invocation", "snap-001");

        writer.insertEdge(edge);

        List<GraphEdge> from = reader.findEdgesFrom("n1");
        assertThat(from).hasSize(1);
        assertThat(from.get(0).kind()).isEqualTo(EdgeKind.CALLS);
        assertThat(from.get(0).dstNodeId()).isEqualTo("n2");

        List<GraphEdge> to = reader.findEdgesTo("n2");
        assertThat(to).hasSize(1);
        assertThat(to.get(0).srcNodeId()).isEqualTo("n1");
    }

    @Test
    void findNodesByKindAndFile() {
        insertTwoNodes();

        List<GraphNode> classes = reader.findNodesByKind(NodeKind.CLASS);
        assertThat(classes).hasSize(1).extracting(GraphNode::id).containsExactly("n1");

        List<GraphNode> methods = reader.findNodesByKind(NodeKind.METHOD);
        assertThat(methods).hasSize(1).extracting(GraphNode::id).containsExactly("n2");

        List<GraphNode> byFile = reader.findNodesByFile("src/main/java/com/example/Foo.java");
        assertThat(byFile).hasSize(2);
    }

    @Test
    void findNodeBySignature() {
        insertTwoNodes();

        assertThat(reader.findNodeBySignature("com.example.Foo")).isPresent();
        assertThat(reader.findNodeBySignature("nonexistent")).isEmpty();
    }

    @Test
    void ftsSearchFindsMatchingNodes() {
        insertTwoNodes();
        writer.syncFts();

        List<GraphNode> results = reader.searchNodes("example");
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(GraphNode::id).contains("n1");
    }

    @Test
    void bulkInsertNodes() {
        List<GraphNode> batch = List.of(
                new GraphNode("b1", NodeKind.CLASS, "com.a.A", null,
                        "A.java", null, null, "snap-002"),
                new GraphNode("b2", NodeKind.CLASS, "com.a.B", null,
                        "B.java", null, null, "snap-002"),
                new GraphNode("b3", NodeKind.INTERFACE, "com.a.C", null,
                        "C.java", null, null, "snap-002"));

        writer.bulkInsertNodes(batch);

        assertThat(reader.countNodes()).isEqualTo(3);
        assertThat(reader.findNodesByKind(NodeKind.CLASS)).hasSize(2);
        assertThat(reader.findNodesByKind(NodeKind.INTERFACE)).hasSize(1);
    }

    @Test
    void bulkInsertEdges() {
        insertTwoNodes();

        List<GraphEdge> batch = List.of(
                new GraphEdge("be1", "n1", "n2", EdgeKind.CALLS,
                        Confidence.HIGH, EvidenceSource.SOLVER,
                        "Foo.java", new SourceRange(10, 0, 10, 20),
                        "call1", "snap-001"),
                new GraphEdge("be2", "n1", "n2", EdgeKind.CONTAINS,
                        Confidence.HIGH, EvidenceSource.SOLVER,
                        "Foo.java", new SourceRange(1, 0, 42, 1),
                        "contains", "snap-001"));

        writer.bulkInsertEdges(batch);

        assertThat(reader.countEdges()).isEqualTo(2);
        assertThat(reader.findEdgesByKind(EdgeKind.CALLS)).hasSize(1);
    }

    @Test
    void fingerprintCrud() {
        writer.insertFingerprint("Foo.java", "abc123", "snap-001");

        assertThat(reader.getFingerprint("Foo.java")).contains("abc123");
        assertThat(reader.getFingerprint("Bar.java")).isEmpty();

        writer.insertFingerprint("Bar.java", "def456", "snap-001");

        Map<String, String> all = reader.getAllFingerprints();
        assertThat(all).hasSize(2)
                .containsEntry("Foo.java", "abc123")
                .containsEntry("Bar.java", "def456");
    }

    @Test
    void fingerprintUpsertOverwritesExisting() {
        writer.insertFingerprint("Foo.java", "v1", "snap-001");
        writer.insertFingerprint("Foo.java", "v2", "snap-002");

        assertThat(reader.getFingerprint("Foo.java")).contains("v2");
        assertThat(reader.getAllFingerprints()).hasSize(1);
    }

    @Test
    void snapshotMeta() {
        writer.insertSnapshotMeta("snap-001", "2026-05-10T12:00:00Z",
                "1", "0.1.0", "full", true);

        assertThat(reader.countNodes()).isZero();
    }

    @Test
    void countReflectsInserts() {
        assertThat(reader.countNodes()).isZero();
        assertThat(reader.countEdges()).isZero();

        insertTwoNodes();
        assertThat(reader.countNodes()).isEqualTo(2);

        writer.insertEdge(new GraphEdge(
                "e1", "n1", "n2", EdgeKind.CALLS,
                Confidence.HIGH, EvidenceSource.SOLVER,
                "Foo.java", new SourceRange(10, 0, 10, 20),
                "call", "snap-001"));
        assertThat(reader.countEdges()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void insertTwoNodes() {
        writer.insertNode(new GraphNode(
                "n1", NodeKind.CLASS,
                "com.example.Foo", null,
                "src/main/java/com/example/Foo.java",
                new SourceRange(1, 0, 42, 1),
                null, "snap-001"));
        writer.insertNode(new GraphNode(
                "n2", NodeKind.METHOD,
                "com.example.Foo#bar()", null,
                "src/main/java/com/example/Foo.java",
                new SourceRange(10, 4, 15, 5),
                null, "snap-001"));
    }
}
