package com.sgarsgaya.codeatlas.core.spring;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sgarsgaya.codeatlas.core.indexer.EdgeCandidate;
import com.sgarsgaya.codeatlas.core.indexer.IndexedSymbols;
import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.model.SourceRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Spring stereotypes, HTTP endpoints, and basic bean wiring hints. */
public final class SpringEnricher {

    private static final SourceRange SYNTH = EdgeCandidate.openapiSyntheticRange();

    private final String snapshotId;

    public SpringEnricher(String snapshotId) {
        this.snapshotId = Objects.requireNonNull(snapshotId);
    }

    public SpringEnrichmentResult enrich(IndexedSymbols indexed) {
        List<GraphNode> extraNodes = new ArrayList<>();
        List<EdgeCandidate> extraEdges = new ArrayList<>();
        Map<String, String> beanTypeToNodeId = new HashMap<>();
        List<HttpEndpointInfo> endpoints = new ArrayList<>();

        Map<String, String> stereotypeBeansByFqType = new LinkedHashMap<>();
        Map<String, String> classBasePathByFqType = new LinkedHashMap<>();

        for (GraphNode node : indexed.nodes()) {
            if (node.kind() != NodeKind.CLASS && node.kind() != NodeKind.INTERFACE) {
                continue;
            }
            JsonObject attrs = parseJsonObject(node.attributesJson());
            JsonArray springDetails = attrs == null ? null : attrs.getAsJsonArray("springDetails");
            if (springDetails == null) {
                continue;
            }

            String fq = node.fqSignature();
            if (fq == null || fq.isBlank()) {
                continue;
            }

            Optional<String> basePath = extractRequestMappingBase(springDetails);
            basePath.ifPresent(p -> classBasePathByFqType.put(fq, p));

            if (hasAny(springDetails, "RestController", SpringAnnotation.REST_CONTROLLER.simpleName())) {
                addStereotype(
                        fq,
                        NodeKind.CONTROLLER,
                        "spring:controller:",
                        stereotypeBeansByFqType,
                        extraNodes,
                        extraEdges,
                        node.id(),
                        node.filePath());
            } else if (hasAny(springDetails, "Controller", SpringAnnotation.CONTROLLER.simpleName())) {
                addStereotype(
                        fq,
                        NodeKind.CONTROLLER,
                        "spring:controller:",
                        stereotypeBeansByFqType,
                        extraNodes,
                        extraEdges,
                        node.id(),
                        node.filePath());
            } else if (hasAny(springDetails, "Service", SpringAnnotation.SERVICE.simpleName())) {
                addStereotype(
                        fq,
                        NodeKind.SERVICE,
                        "spring:service:",
                        stereotypeBeansByFqType,
                        extraNodes,
                        extraEdges,
                        node.id(),
                        node.filePath());
            } else if (hasAny(springDetails, "Repository", SpringAnnotation.REPOSITORY_ANN.simpleName())) {
                addStereotype(
                        fq,
                        NodeKind.REPOSITORY_BEAN,
                        "spring:repository:",
                        stereotypeBeansByFqType,
                        extraNodes,
                        extraEdges,
                        node.id(),
                        node.filePath());
            } else if (hasAny(springDetails, "Component", SpringAnnotation.COMPONENT.simpleName())) {
                addStereotype(
                        fq,
                        NodeKind.SPRING_BEAN,
                        "spring:component:",
                        stereotypeBeansByFqType,
                        extraNodes,
                        extraEdges,
                        node.id(),
                        node.filePath());
            }

            Optional.ofNullable(node.fqSignature()).ifPresent(s -> beanTypeToNodeId.putIfAbsent(s, node.id()));
        }

        for (GraphNode node : indexed.nodes()) {
            if (node.kind() != NodeKind.METHOD) {
                continue;
            }
            JsonObject attrs = parseJsonObject(node.attributesJson());
            JsonArray springDetails = attrs == null ? null : attrs.getAsJsonArray("springDetails");
            if (springDetails == null || springDetails.isEmpty()) {
                continue;
            }

            String primary = node.fqSignature();
            if (primary == null || !primary.contains("#")) {
                continue;
            }
            String fqClass = primary.substring(0, primary.indexOf('#'));
            String methodName = primary.substring(primary.indexOf('#') + 1, primary.indexOf('('));

            String base = classBasePathByFqType.getOrDefault(fqClass, "");

            Optional<MappedRoute> route = mapWebRoute(springDetails);
            if (route.isEmpty()) {
                if (hasTransactional(springDetails)) {
                    String tbId = "spring:tx:" + primary;
                    extraNodes.add(new GraphNode(
                            tbId,
                            NodeKind.TRANSACTION_BOUNDARY,
                            primary,
                            null,
                            node.filePath(),
                            node.sourceRange(),
                            "{}",
                            snapshotId));
                    extraEdges.add(EdgeCandidate.builder(node.id(), tbId, EdgeKind.ANNOTATED_WITH, safeFile(node))
                            .evidenceSource(EvidenceSource.SPRING_CONVENTION)
                            .confidence(Confidence.MEDIUM)
                            .sourceRange(node.sourceRange() != null ? node.sourceRange() : SYNTH)
                            .reason("transactional_method")
                            .build());
                }
                if (hasScheduled(springDetails)) {
                    String jobId = "spring:sched:" + primary;
                    extraNodes.add(new GraphNode(
                            jobId,
                            NodeKind.SCHEDULED_JOB,
                            primary,
                            null,
                            node.filePath(),
                            node.sourceRange(),
                            "{}",
                            snapshotId));
                    extraEdges.add(EdgeCandidate.builder(node.id(), jobId, EdgeKind.ANNOTATED_WITH, safeFile(node))
                            .evidenceSource(EvidenceSource.SPRING_CONVENTION)
                            .confidence(Confidence.MEDIUM)
                            .sourceRange(node.sourceRange() != null ? node.sourceRange() : SYNTH)
                            .reason("scheduled_method")
                            .build());
                }
                continue;
            }

            MappedRoute mr = route.orElseThrow();
            String fullPath = normalizePath(joinPaths(base, mr.path()));
            String httpEndpointId = "http:" + mr.httpMethod() + ":" + fullPath + ":" + fqClass + "#" + methodName;

            extraNodes.add(new GraphNode(
                    httpEndpointId,
                    NodeKind.HTTP_ENDPOINT,
                    httpEndpointId,
                    null,
                    node.filePath(),
                    node.sourceRange(),
                    "{\"httpMethod\":\"%s\",\"path\":\"%s\"}"
                            .formatted(mr.httpMethod(), fullPath.replace("\"", "\\\"")),
                    snapshotId));

            extraEdges.add(EdgeCandidate.builder(node.id(), httpEndpointId, EdgeKind.EXPOSES_ENDPOINT, safeFile(node))
                    .evidenceSource(EvidenceSource.SPRING_CONVENTION)
                    .confidence(Confidence.MEDIUM)
                    .sourceRange(node.sourceRange() != null ? node.sourceRange() : SYNTH)
                    .reason("spring_web_mapping")
                    .build());

            endpoints.add(new HttpEndpointInfo(
                    httpEndpointId, mr.httpMethod(), fullPath, fqClass, methodName, node.id()));
        }

        stereotypeBeansByFqType.forEach((fq, beanId) -> beanTypeToNodeId.putIfAbsent(fq, beanId));

        return new SpringEnrichmentResult(extraNodes, extraEdges, beanTypeToNodeId, endpoints);
    }

    private static String safeFile(GraphNode node) {
        return node.filePath() != null ? node.filePath() : ".";
    }

    private void addStereotype(
            String fqType,
            NodeKind kind,
            String idPrefix,
            Map<String, String> stereotypeBeansByFqType,
            List<GraphNode> extraNodes,
            List<EdgeCandidate> extraEdges,
            String typeNodeId,
            String filePath) {

        String beanId = idPrefix + fqType;
        stereotypeBeansByFqType.put(fqType, beanId);

        extraNodes.add(new GraphNode(
                beanId, kind, fqType, null, filePath, SYNTH, "{}", snapshotId));

        extraEdges.add(EdgeCandidate.builder(typeNodeId, beanId, EdgeKind.PROVIDES_BEAN, filePath != null ? filePath : ".")
                .evidenceSource(EvidenceSource.SPRING_CONVENTION)
                .confidence(Confidence.MEDIUM)
                .sourceRange(SYNTH)
                .reason("spring_stereotype")
                .build());
    }

    private static boolean hasTransactional(JsonArray details) {
        return hasAny(details, "Transactional", SpringAnnotation.TRANSACTIONAL.simpleName());
    }

    private static boolean hasScheduled(JsonArray details) {
        return hasAny(details, "Scheduled", SpringAnnotation.SCHEDULED.simpleName());
    }

    private static boolean hasAny(JsonArray details, String... simpleNames) {
        Set<String> want = new HashSet<>();
        for (String name : simpleNames) {
            want.add(name);
        }
        for (JsonElement el : details) {
            if (!el.isJsonObject()) {
                continue;
            }
            String simple = el.getAsJsonObject().get("simple").getAsString();
            if (want.contains(simple)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> extractRequestMappingBase(JsonArray details) {
        for (JsonElement el : details) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String simple = o.get("simple").getAsString();
            if (!"RequestMapping".equals(simple)) {
                continue;
            }
            Optional<String> p = readPathFromAnnotationObject(o);
            if (p.isPresent()) {
                return p;
            }
        }
        return Optional.empty();
    }

    private record MappedRoute(String httpMethod, String path) {}

    private static Optional<MappedRoute> mapWebRoute(JsonArray details) {
        for (JsonElement el : details) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String simple = o.get("simple").getAsString();
            Optional<String> path = readPathFromAnnotationObject(o);
            MappedRoute mapped =
                    switch (simple) {
                        case "GetMapping" -> new MappedRoute("GET", path.orElse("/"));
                        case "PostMapping" -> new MappedRoute("POST", path.orElse("/"));
                        case "PutMapping" -> new MappedRoute("PUT", path.orElse("/"));
                        case "DeleteMapping" -> new MappedRoute("DELETE", path.orElse("/"));
                        case "PatchMapping" -> new MappedRoute("PATCH", path.orElse("/"));
                        case "RequestMapping" -> new MappedRoute(
                                readStringPair(o, "method")
                                        .or(() -> readEnumMethod(o))
                                        .orElse("GET")
                                        .toUpperCase(Locale.ROOT),
                                path.orElse("/"));
                        default -> null;
                    };
            if (mapped != null) {
                return Optional.of(mapped);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> readEnumMethod(JsonObject ann) {
        if (!ann.has("pairs") || !ann.get("pairs").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject pairs = ann.getAsJsonObject("pairs");
        if (!pairs.has("method")) {
            return Optional.empty();
        }
        JsonElement m = pairs.get("method");
        // often RequestMethod.GET
        String raw = m.getAsString();
        int dot = raw.lastIndexOf('.');
        String name = dot >= 0 ? raw.substring(dot + 1) : raw;
        return Optional.of(name);
    }

    private static Optional<String> readStringPair(JsonObject ann, String key) {
        if (!ann.has("pairs") || !ann.get("pairs").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject pairs = ann.getAsJsonObject("pairs");
        if (!pairs.has(key)) {
            return Optional.empty();
        }
        JsonElement v = pairs.get(key);
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
            return Optional.of(v.getAsString());
        }
        return Optional.of(v.toString());
    }

    private static Optional<String> readPathFromAnnotationObject(JsonObject ann) {
        if (ann.has("value")) {
            JsonElement v = ann.get("value");
            if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                return Optional.of(v.getAsString());
            }
            if (v.isJsonArray() && !v.getAsJsonArray().isEmpty()) {
                return Optional.of(v.getAsJsonArray().get(0).getAsString());
            }
        }
        if (ann.has("pairs") && ann.get("pairs").isJsonObject()) {
            JsonObject pairs = ann.getAsJsonObject("pairs");
            for (String k : List.of("value", "path")) {
                if (!pairs.has(k)) {
                    continue;
                }
                JsonElement v = pairs.get(k);
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                    return Optional.of(v.getAsString());
                }
                if (v.isJsonArray() && !v.getAsJsonArray().isEmpty()) {
                    return Optional.of(v.getAsJsonArray().get(0).getAsString());
                }
            }
        }
        return Optional.empty();
    }

    private static JsonObject parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String joinPaths(String base, String path) {
        if (base == null || base.isBlank()) {
            return path == null ? "/" : path;
        }
        if (path == null || path.isBlank()) {
            return base;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }

    private static String normalizePath(String p) {
        if (p == null || p.isBlank()) {
            return "/";
        }
        if (!p.startsWith("/")) {
            return "/" + p;
        }
        return p;
    }
}
