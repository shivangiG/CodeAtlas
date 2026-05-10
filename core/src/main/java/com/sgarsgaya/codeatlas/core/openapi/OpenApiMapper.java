package com.sgarsgaya.codeatlas.core.openapi;

import com.sgarsgaya.codeatlas.core.indexer.EdgeCandidate;
import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.spring.HttpEndpointInfo;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
public final class OpenApiMapper {

    private final String snapshotId;

    public OpenApiMapper(String snapshotId) {
        this.snapshotId = Objects.requireNonNull(snapshotId);
    }

    public OpenApiMapResult map(List<Path> specFiles, List<HttpEndpointInfo> endpoints) {
        OpenApiMapResult acc = new OpenApiMapResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Path spec : specFiles) {
            OpenApiMapResult partial = mapSpec(spec, endpoints);
            acc.nodes().addAll(partial.nodes());
            acc.edgeCandidates().addAll(partial.edgeCandidates());
            acc.mappings().addAll(partial.mappings());
            acc.ambiguities().addAll(partial.ambiguities());
        }
        return acc;
    }

    public OpenApiMapResult mapSpec(Path specFile, List<HttpEndpointInfo> endpoints) {
        List<GraphNode> nodes = new ArrayList<>();
        List<EdgeCandidate> edges = new ArrayList<>();
        List<OperationMapping> mappings = new ArrayList<>();
        List<AmbiguityReport> ambiguities = new ArrayList<>();

        OpenAPI openApi = new OpenAPIV3Parser().read(specFile.toUri().toString());
        if (openApi == null || openApi.getPaths() == null) {
            ambiguities.add(new AmbiguityReport(
                    "<missing>", "<missing>", "<missing>", List.of(), null, "openapi_parse_failed_or_empty"));
            return new OpenApiMapResult(nodes, edges, mappings, ambiguities);
        }

        String posixSpec = specFile.toString().replace('\\', '/');

        openApi.getPaths().forEach((rawPath, pathItem) -> {
            if (pathItem == null) {
                return;
            }
            mapOperation(pathItem.getGet(), "GET", rawPath, posixSpec, endpoints, nodes, edges, mappings, ambiguities);
            mapOperation(pathItem.getPost(), "POST", rawPath, posixSpec, endpoints, nodes, edges, mappings, ambiguities);
            mapOperation(pathItem.getPut(), "PUT", rawPath, posixSpec, endpoints, nodes, edges, mappings, ambiguities);
            mapOperation(pathItem.getDelete(), "DELETE", rawPath, posixSpec, endpoints, nodes, edges, mappings, ambiguities);
            mapOperation(pathItem.getPatch(), "PATCH", rawPath, posixSpec, endpoints, nodes, edges, mappings, ambiguities);
        });

        return new OpenApiMapResult(nodes, edges, mappings, ambiguities);
    }

    private void mapOperation(
            Operation op,
            String httpMethod,
            String rawPath,
            String posixSpec,
            List<HttpEndpointInfo> endpoints,
            List<GraphNode> nodes,
            List<EdgeCandidate> edges,
            List<OperationMapping> mappings,
            List<AmbiguityReport> ambiguities) {

        if (op == null) {
            return;
        }

        String normPath = normalizePath(rawPath);
        String opId = op.getOperationId() != null ? op.getOperationId() : (httpMethod + "_" + normPath.replace('/', '_'));
        String openapiNodeId = "openapiop:" + httpMethod + ":" + normPath + ":" + safeId(opId);

        String attrs =
                "{\"operationId\":\"%s\",\"httpMethod\":\"%s\",\"path\":\"%s\"}"
                        .formatted(safeJson(opId), httpMethod, safeJson(normPath));

        nodes.add(new GraphNode(
                openapiNodeId,
                NodeKind.OPENAPI_OPERATION,
                opId,
                null,
                posixSpec,
                EdgeCandidate.openapiSyntheticRange(),
                attrs,
                snapshotId));

        List<HttpEndpointInfo> opIdMatches = endpoints.stream()
                .filter(e -> e.httpMethod().equalsIgnoreCase(httpMethod))
                .filter(e -> e.methodName().equalsIgnoreCase(opId))
                .toList();

        List<HttpEndpointInfo> routeMatches = endpoints.stream()
                .filter(e -> e.httpMethod().equalsIgnoreCase(httpMethod))
                .filter(e -> pathsEqual(normPath, normalizePath(e.path())))
                .toList();

        LinkedHashMap<String, HttpEndpointInfo> ranked = new LinkedHashMap<>();
        for (HttpEndpointInfo e : opIdMatches) {
            ranked.put(e.methodNodeId(), e);
        }
        for (HttpEndpointInfo e : routeMatches) {
            ranked.putIfAbsent(e.methodNodeId(), e);
        }
        if (ranked.isEmpty()) {
            for (HttpEndpointInfo e : endpoints) {
                if (!e.httpMethod().equalsIgnoreCase(httpMethod)) {
                    continue;
                }
                if (normalizeToken(e.methodName()).equals(normalizeToken(opId))) {
                    ranked.putIfAbsent(e.methodNodeId(), e);
                }
            }
        }

        if (ranked.isEmpty()) {
            ambiguities.add(new AmbiguityReport(
                    opId,
                    httpMethod,
                    normPath,
                    endpoints.stream().map(HttpEndpointInfo::methodNodeId).toList(),
                    null,
                    "unmapped_openapi_operation"));
            return;
        }

        HttpEndpointInfo chosen = ranked.values().iterator().next();
        Confidence confidence;
        EvidenceSource evidence;
        String reason;

        if (!opIdMatches.isEmpty() && opIdMatches.contains(chosen)) {
            confidence = Confidence.HIGH;
            evidence = EvidenceSource.OPENAPI;
            reason = "openapi_operationId_match";
        } else if (!routeMatches.isEmpty() && routeMatches.contains(chosen)) {
            confidence = Confidence.HIGH;
            evidence = EvidenceSource.OPENAPI;
            reason = "openapi_route_match";
        } else {
            confidence = Confidence.MEDIUM;
            evidence = EvidenceSource.NAME_MATCH;
            reason = "openapi_name_heuristic";
        }

        edges.add(EdgeCandidate.builder(chosen.methodNodeId(), openapiNodeId, EdgeKind.IMPLEMENTS_OPERATION, posixSpec)
                .confidence(confidence)
                .evidenceSource(evidence)
                .sourceRange(EdgeCandidate.openapiSyntheticRange())
                .reason(reason)
                .build());

        mappings.add(new OperationMapping(
                opId,
                httpMethod,
                normPath,
                chosen.methodNodeId(),
                openapiNodeId,
                confidence,
                reason));

        if (ranked.size() > 1) {
            ambiguities.add(new AmbiguityReport(
                    opId,
                    httpMethod,
                    normPath,
                    ranked.keySet().stream().toList(),
                    chosen.methodNodeId(),
                    "ambiguous_openapi_mapping_top_candidate_chosen"));
        }
    }

    private static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/";
        }
        String p = raw.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static boolean pathsEqual(String a, String b) {
        return normalizePath(a).equalsIgnoreCase(normalizePath(b));
    }

    private static String normalizeToken(String s) {
        return s == null
                ? ""
                : s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static String safeId(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private static String safeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
