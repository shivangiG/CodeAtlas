package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.config.RepoAtlasConfig;
import com.sgarsgaya.codeatlas.core.freshness.GraphStatusFile;
import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.openapi.OpenApiMapResult;
import com.sgarsgaya.codeatlas.core.openapi.OpenApiMapper;
import com.sgarsgaya.codeatlas.core.snapshot.SnapshotManager;
import com.sgarsgaya.codeatlas.core.snapshot.SnapshotValidator;
import com.sgarsgaya.codeatlas.core.spring.SpringEnricher;
import com.sgarsgaya.codeatlas.core.spring.SpringEnrichmentResult;
import com.sgarsgaya.codeatlas.core.storage.GraphDatabase;
import com.sgarsgaya.codeatlas.core.storage.GraphWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Coordinates scanning, AST/Spring/OpenAPI stages, edge funneling, and atomic publish. */
public final class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);
    private static final String SCHEMA_VERSION = "1";
    private static final String INDEXER_VERSION = "codeatlas-core/0.1.0";

    private final Path repoRoot;
    private final RepoAtlasConfig config;

    public PipelineOrchestrator(Path repoRoot, RepoAtlasConfig config) {
        this.repoRoot = repoRoot.toAbsolutePath().normalize();
        this.config = Objects.requireNonNullElseGet(config, RepoAtlasConfig::new);
    }

    public BuildResult buildIncremental(List<Path> changedFiles) {
        List<String> diagnostics = new ArrayList<>();
        if (changedFiles != null && !changedFiles.isEmpty()) {
            diagnostics.add("incremental_build_not_implemented: performed full rebuild instead");
        }
        return buildFullWithDiagnostics(diagnostics);
    }

    public BuildResult buildFull() {
        return buildFullWithDiagnostics(new ArrayList<>());
    }

    private BuildResult buildFullWithDiagnostics(List<String> seedDiagnostics) {
        ArrayList<String> diagnostics = new ArrayList<>(seedDiagnostics);
        long t0 = System.currentTimeMillis();

        SnapshotManager snapshotManager = new SnapshotManager();
        Path snapshotPath;
        try {
            snapshotPath = snapshotManager.createNewSnapshot(repoRoot);
        } catch (IOException e) {
            return BuildResult.failure("Failed to allocate snapshot: " + e.getMessage());
        }

        String logicalId = logicalIdFromSnapshotPath(snapshotPath);
        String snapshotFileName = snapshotPath.getFileName().toString();

        try {
            RepositoryScanner scanner = new RepositoryScanner();
            ScanResult scan = scanner.scan(repoRoot, config);

            IndexedSymbols indexed = JavaAstIndexer.indexAll(logicalId, scan);
            for (IndexDiagnostic d : indexed.diagnostics()) {
                diagnostics.add("index:" + d.severity() + ":" + d.filePath() + ":" + d.message());
            }

            SpringEnrichmentResult spring = new SpringEnricher(logicalId).enrich(indexed);
            for (GraphNode n : spring.additionalNodes()) {
                indexed.addNode(n);
            }
            for (var e : spring.additionalEdgeCandidates()) {
                indexed.addEdgeCandidate(e);
            }

            List<Path> openapiSpecs = discoverOpenApiSpecs(repoRoot);
            OpenApiMapper openApiMapper = new OpenApiMapper(logicalId);
            OpenApiMapResult openApi =
                    openapiSpecs.isEmpty() ? OpenApiMapResult.empty() : openApiMapper.map(openapiSpecs, spring.endpoints());
            for (GraphNode n : openApi.nodes()) {
                indexed.addNode(n);
            }
            for (var e : openApi.edgeCandidates()) {
                indexed.addEdgeCandidate(e);
            }
            for (var a : openApi.ambiguities()) {
                diagnostics.add("openapi_ambiguity:" + a.reason() + ":" + a.operationId());
            }

            LinkedHashMap<String, GraphNode> nodesById = new LinkedHashMap<>();
            GraphNode repository = new GraphNode(
                    "repository:root",
                    NodeKind.REPOSITORY,
                    repoRoot.toString(),
                    null,
                    null,
                    null,
                    "{}",
                    logicalId);
            nodesById.put(repository.id(), repository);

            for (GraphNode n : indexed.nodes()) {
                nodesById.putIfAbsent(n.id(), n);
            }

            EdgeFunnel funnel = new EdgeFunnel(logicalId);
            List<GraphEdge> edges = funnel.processAll(indexed.edgeCandidates());
            for (var err : funnel.getErrors()) {
                diagnostics.add("edge_funnel:" + err.field() + ":" + err.message());
            }

            try (GraphDatabase db = new GraphDatabase(snapshotPath)) {
                db.initSchema();
                GraphWriter writer = new GraphWriter(db);
                writer.bulkInsertNodes(new ArrayList<>(nodesById.values()));
                writer.bulkInsertEdges(edges);

                for (Map.Entry<String, String> fp : scan.fingerprints().entrySet()) {
                    writer.insertFingerprint(fp.getKey(), fp.getValue(), logicalId);
                }

                writer.insertSnapshotMeta(
                        logicalId,
                        OffsetDateTime.now().toString(),
                        SCHEMA_VERSION,
                        INDEXER_VERSION,
                        "full",
                        true);
                writer.syncFts();
            }

            var validation = SnapshotValidator.validate(snapshotPath);
            if (!validation.valid()) {
                diagnostics.addAll(validation.errors());
                Files.deleteIfExists(snapshotPath);
                long ms = System.currentTimeMillis() - t0;
                return new BuildResult(false, logicalId, snapshotFileName, 0, 0, scan.sourceFiles().size(), ms, diagnostics, List.of());
            }

            boolean published = snapshotManager.publish(repoRoot, snapshotPath);
            if (!published) {
                diagnostics.add("publish_failed: see logs (lock held or validation rejection)");
                long ms = System.currentTimeMillis() - t0;
                return new BuildResult(false, logicalId, snapshotFileName, nodesById.size(), edges.size(), scan.sourceFiles().size(), ms, diagnostics, List.of());
            }

            GraphStatusFile status = GraphStatusFile.read(repoRoot);
            status.setActiveSnapshotId(snapshotFileName);
            status.setFreshness("fresh");
            status.setRebuildWorkerActive(false);
            status.setLastBuildTime(OffsetDateTime.now().toString());
            status.setLastBuildDuration(String.valueOf(System.currentTimeMillis() - t0));
            status.setNodeCount(nodesById.size());
            status.setEdgeCount(edges.size());
            status.write(repoRoot);

            long ms = System.currentTimeMillis() - t0;
            List<String> ambiguities = openApi.ambiguities().stream()
                    .map(a -> a.reason() + ":" + a.operationId() + ":" + a.httpMethod() + ":" + a.path())
                    .toList();
            return new BuildResult(
                    true,
                    logicalId,
                    snapshotFileName,
                    nodesById.size(),
                    edges.size(),
                    scan.sourceFiles().size(),
                    ms,
                    diagnostics,
                    ambiguities);

        } catch (Exception e) {
            log.warn("Pipeline failed", e);
            try {
                Files.deleteIfExists(snapshotPath);
            } catch (IOException io) {
                log.warn("Failed deleting partial snapshot: {}", io.getMessage());
            }
            long ms = System.currentTimeMillis() - t0;
            diagnostics.add("pipeline_exception:" + e.getMessage());
            return new BuildResult(false, logicalId, snapshotFileName, 0, 0, 0, ms, diagnostics, List.of());
        }
    }

    private static String logicalIdFromSnapshotPath(Path snapshotPath) {
        String fn = snapshotPath.getFileName().toString();
        if (fn.startsWith("graph_") && fn.endsWith(".sqlite")) {
            return fn.substring("graph_".length(), fn.length() - ".sqlite".length());
        }
        return fn;
    }

    private static List<Path> discoverOpenApiSpecs(Path repoRoot) throws IOException {
        List<Path> specs = new ArrayList<>();
        Path openapiDir = repoRoot.resolve("openapi-spec");
        if (Files.isDirectory(openapiDir)) {
            try (var paths = Files.walk(openapiDir)) {
                paths.filter(Files::isRegularFile).forEach(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    if (n.endsWith(".yaml") || n.endsWith(".yml") || n.endsWith(".json")) {
                        specs.add(p);
                    }
                });
            }
        }
        return specs;
    }
}
