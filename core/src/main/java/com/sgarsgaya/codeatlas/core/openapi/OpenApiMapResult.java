package com.sgarsgaya.codeatlas.core.openapi;

import com.sgarsgaya.codeatlas.core.indexer.EdgeCandidate;
import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.GraphNode;

import java.util.ArrayList;
import java.util.List;

public final class OpenApiMapResult {

    private final List<GraphNode> nodes;
    private final List<EdgeCandidate> edgeCandidates;
    private final List<OperationMapping> mappings;
    private final List<AmbiguityReport> ambiguities;

    public OpenApiMapResult(
            List<GraphNode> nodes,
            List<EdgeCandidate> edgeCandidates,
            List<OperationMapping> mappings,
            List<AmbiguityReport> ambiguities) {
        this.nodes = nodes;
        this.edgeCandidates = edgeCandidates;
        this.mappings = mappings;
        this.ambiguities = ambiguities;
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public List<EdgeCandidate> edgeCandidates() {
        return edgeCandidates;
    }

    public List<OperationMapping> mappings() {
        return mappings;
    }

    public List<AmbiguityReport> ambiguities() {
        return ambiguities;
    }

    /** Mutable merge helper for composition. */
    public static OpenApiMapResult empty() {
        return new OpenApiMapResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
