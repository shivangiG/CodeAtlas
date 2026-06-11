package com.sgarsgaya.codeatlas.core.model;

public record GraphEdge(
        String id,
        String srcNodeId,
        String dstNodeId,
        EdgeKind kind,
        Confidence confidence,
        EvidenceSource evidenceSource,
        String sourceFile,
        SourceRange sourceRange,
        String reason,
        String snapshotId) {
}
