package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.SourceRange;

/**
 * Candidate edge before funnel validation (required metadata may be inferred).
 */
public final class EdgeCandidate {

    private final String srcNodeId;
    private final String dstNodeId;
    private final EdgeKind kind;
    private final Confidence confidence;
    private final EvidenceSource evidenceSource;
    private final String sourceFile;
    private final SourceRange sourceRange;
    private final String reason;

    private EdgeCandidate(Builder builder) {
        this.srcNodeId = builder.srcNodeId;
        this.dstNodeId = builder.dstNodeId;
        this.kind = builder.kind;
        this.confidence = builder.confidence;
        this.evidenceSource = builder.evidenceSource;
        this.sourceFile = builder.sourceFile;
        this.sourceRange = builder.sourceRange;
        this.reason = builder.reason;
    }

    public String srcNodeId() {
        return srcNodeId;
    }

    public String dstNodeId() {
        return dstNodeId;
    }

    public EdgeKind kind() {
        return kind;
    }

    public Confidence confidence() {
        return confidence;
    }

    public EvidenceSource evidenceSource() {
        return evidenceSource;
    }

    public String sourceFile() {
        return sourceFile;
    }

    public SourceRange sourceRange() {
        return sourceRange;
    }

    public String reason() {
        return reason;
    }

    /** Synthetic range when no Java source coordinates exist (e.g. YAML-only tooling). */
    public static SourceRange openapiSyntheticRange() {
        return new SourceRange(1, 1, 1, 1);
    }

    public static Builder builder(String srcNodeId, String dstNodeId, EdgeKind kind, String sourceFile) {
        return new Builder(srcNodeId, dstNodeId, kind, sourceFile);
    }

    public static final class Builder {
        private final String srcNodeId;
        private final String dstNodeId;
        private final EdgeKind kind;
        private final String sourceFile;

        private Confidence confidence;
        private EvidenceSource evidenceSource;
        private SourceRange sourceRange;
        private String reason;

        private Builder(String srcNodeId, String dstNodeId, EdgeKind kind, String sourceFile) {
            this.srcNodeId = srcNodeId;
            this.dstNodeId = dstNodeId;
            this.kind = kind;
            this.sourceFile = sourceFile;
        }

        public Builder confidence(Confidence confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder evidenceSource(EvidenceSource evidenceSource) {
            this.evidenceSource = evidenceSource;
            return this;
        }

        public Builder sourceRange(SourceRange sourceRange) {
            this.sourceRange = sourceRange;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public EdgeCandidate build() {
            return new EdgeCandidate(this);
        }
    }

    static String defaultReason(String srcNodeId, String dstNodeId, EdgeKind kind) {
        return "edge_kind:" + kind + " from:" + srcNodeId + " to:" + dstNodeId;
    }
}
