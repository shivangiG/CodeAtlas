package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.model.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable aggregation of nodes + edge candidates produced by indexing stages. */
public final class IndexedSymbols {

    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<EdgeCandidate> edgeCandidates = new ArrayList<>();
    private final List<IndexDiagnostic> diagnostics = new ArrayList<>();

    public void addNode(GraphNode node) {
        nodes.add(node);
    }

    public void addEdgeCandidate(EdgeCandidate candidate) {
        edgeCandidates.add(candidate);
    }

    public void addDiagnostic(IndexDiagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public List<GraphNode> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    /** Mutable accessor for merges (within module). */
    List<GraphNode> mutableNodes() {
        return nodes;
    }

    List<EdgeCandidate> mutableEdges() {
        return edgeCandidates;
    }

    public List<EdgeCandidate> edgeCandidates() {
        return Collections.unmodifiableList(edgeCandidates);
    }

    public List<IndexDiagnostic> diagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    public void mergeFrom(IndexedSymbols other) {
        nodes.addAll(other.nodes);
        edgeCandidates.addAll(other.edgeCandidates);
        diagnostics.addAll(other.diagnostics);
    }
}
