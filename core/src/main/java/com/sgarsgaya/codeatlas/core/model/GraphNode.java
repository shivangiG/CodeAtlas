package com.sgarsgaya.codeatlas.core.model;

public record GraphNode(
        String id,
        NodeKind kind,
        String fqSignature,
        String fallbackKey,
        String filePath,
        SourceRange sourceRange,
        String attributesJson,
        String snapshotId) {
}
