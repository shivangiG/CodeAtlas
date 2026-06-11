package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.SourceRange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Single validation path for all persisted edges (design §9.5). */
public final class EdgeFunnel {

    private static final Logger log = LoggerFactory.getLogger(EdgeFunnel.class);
    private static final SourceRange SYNTHETIC_JAVA = new SourceRange(0, 0, 0, 0);

    private final String snapshotId;
    private final List<EdgeValidationError> errors = new ArrayList<>();

    public EdgeFunnel(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public List<EdgeValidationError> getErrors() {
        return List.copyOf(errors);
    }

    public GraphEdge process(EdgeCandidate candidate) {
        return processInternal(candidate).orElse(null);
    }

    public List<GraphEdge> processAll(List<EdgeCandidate> candidates) {
        List<GraphEdge> edges = new ArrayList<>();
        for (EdgeCandidate c : candidates) {
            processInternal(c).ifPresent(edges::add);
        }
        return edges;
    }

    private java.util.Optional<GraphEdge> processInternal(EdgeCandidate candidate) {
        if (isBlank(candidate.srcNodeId())) {
            reject(candidate, "srcNodeId", "srcNodeId must not be blank");
            return java.util.Optional.empty();
        }
        if (isBlank(candidate.dstNodeId())) {
            reject(candidate, "dstNodeId", "dstNodeId must not be blank");
            return java.util.Optional.empty();
        }
        if (candidate.kind() == null) {
            reject(candidate, "kind", "kind must not be null");
            return java.util.Optional.empty();
        }
        if (isBlank(candidate.sourceFile())) {
            reject(candidate, "sourceFile", "sourceFile must not be blank");
            return java.util.Optional.empty();
        }

        EvidenceSource evidence = candidate.evidenceSource() != null ? candidate.evidenceSource() : EvidenceSource.NAME_MATCH;
        Confidence confidence = candidate.confidence() != null
                ? candidate.confidence()
                : EdgeConfidenceRules.defaultConfidence(evidence);
        SourceRange sourceRange =
                candidate.sourceRange() != null ? candidate.sourceRange() : openapiSyntheticFallback(candidate.kind());

        String reason = candidate.reason() != null && !candidate.reason().isBlank()
                ? candidate.reason()
                : EdgeCandidate.defaultReason(candidate.srcNodeId(), candidate.dstNodeId(), candidate.kind());

        return java.util.Optional.of(new GraphEdge(
                UUID.randomUUID().toString(),
                candidate.srcNodeId(),
                candidate.dstNodeId(),
                candidate.kind(),
                confidence,
                evidence,
                candidate.sourceFile(),
                sourceRange,
                reason,
                snapshotId));
    }

    private static SourceRange openapiSyntheticFallback(EdgeKind kind) {
        if (kind == EdgeKind.IMPLEMENTS_OPERATION || kind == EdgeKind.DEPENDS_ON) {
            return EdgeCandidate.openapiSyntheticRange();
        }
        return SYNTHETIC_JAVA;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void reject(EdgeCandidate candidate, String field, String message) {
        errors.add(new EdgeValidationError(candidate, field, message));
        log.warn("Rejected edge candidate: {} [{}] {}", field, message, candidate.kind());
    }
}
