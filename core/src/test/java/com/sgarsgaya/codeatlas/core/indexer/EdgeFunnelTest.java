package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphEdge;
import com.sgarsgaya.codeatlas.core.model.SourceRange;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeFunnelTest {

    @Test
    void rejects_blank_src_node() {
        EdgeFunnel funnel = new EdgeFunnel("S1");
        EdgeCandidate cand = EdgeCandidate.builder("", "dst", EdgeKind.CALLS, "A.java").build();

        GraphEdge edge = funnel.process(cand);

        assertThat(edge).isNull();
        assertThat(funnel.getErrors()).hasSize(1);
    }

    @Test
    void assigns_defaults_when_missing_evidence_and_confidence_and_reason() {
        EdgeFunnel funnel = new EdgeFunnel("S1");
        EdgeCandidate cand = EdgeCandidate.builder("a", "b", EdgeKind.CALLS, "A.java")
                .confidence(null)
                .evidenceSource(null)
                .reason(null)
                .build();

        GraphEdge edge = funnel.process(cand);

        assertThat(edge).isNotNull();
        assertThat(edge.confidence()).isEqualTo(Confidence.LOW);
        assertThat(edge.evidenceSource()).isEqualTo(EvidenceSource.NAME_MATCH);
        assertThat(edge.reason()).contains("edge_kind:CALLS");
    }

    @Test
    void batch_splits_valid_and_invalid() {
        EdgeFunnel funnel = new EdgeFunnel("S1");

        EdgeCandidate missingFile = EdgeCandidate.builder("a", "b", EdgeKind.CALLS, "")
                .evidenceSource(EvidenceSource.SOLVER)
                .build();

        EdgeCandidate ok = EdgeCandidate.builder("a", "b", EdgeKind.CALLS, "A.java")
                .evidenceSource(EvidenceSource.SOLVER)
                .confidence(Confidence.HIGH)
                .sourceRange(new SourceRange(1, 1, 1, 1))
                .reason("reason")
                .build();

        List<GraphEdge> edges = funnel.processAll(List.of(missingFile, ok));

        assertThat(edges).hasSize(1);
        assertThat(funnel.getErrors()).hasSize(1);
    }

    @Test
    void propagation_minimum_is_weakest_confidence_along_paths() {
        assertThat(
                        EdgeConfidenceRules.minimumAlongPath(
                                List.of(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW)))
                .isEqualTo(Confidence.LOW);
        assertThat(
                        EdgeConfidenceRules.minimumAlongPath(Set.of(Confidence.HIGH, Confidence.MEDIUM)))
                .isEqualTo(Confidence.MEDIUM);
        assertThat(EdgeConfidenceRules.degradeByOneTier(Confidence.HIGH)).isEqualTo(Confidence.MEDIUM);
        assertThat(EdgeConfidenceRules.degradeByOneTier(Confidence.LOW)).isEqualTo(Confidence.LOW);
    }
}
