package com.sgarsgaya.codeatlas.core.spring;

import com.sgarsgaya.codeatlas.core.indexer.EdgeCandidate;
import com.sgarsgaya.codeatlas.core.model.GraphNode;

import java.util.List;
import java.util.Map;

public record SpringEnrichmentResult(
        List<GraphNode> additionalNodes,
        List<EdgeCandidate> additionalEdgeCandidates,
        Map<String, String> beanTypeToNodeId,
        List<HttpEndpointInfo> endpoints) {}
