package com.sgarsgaya.codeatlas.core.indexer;

import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Confidence defaults and propagation per design (indexing funnel). */
public final class EdgeConfidenceRules {

    private static final EnumSet<EvidenceSource> HIGH_DEFAULT =
            EnumSet.of(EvidenceSource.SOLVER, EvidenceSource.MANIFEST, EvidenceSource.OPENAPI);
    private static final EnumSet<EvidenceSource> MEDIUM_DEFAULT = EnumSet.of(
            EvidenceSource.SPRING_CONVENTION,
            EvidenceSource.FEIGN,
            EvidenceSource.RETROFIT,
            EvidenceSource.WEBCLIENT,
            EvidenceSource.REST_TEMPLATE,
            EvidenceSource.TEST_EVIDENCE);

    private EdgeConfidenceRules() {}

    public static Confidence defaultConfidence(EvidenceSource source) {
        if (HIGH_DEFAULT.contains(source)) {
            return Confidence.HIGH;
        }
        if (MEDIUM_DEFAULT.contains(source)) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    /** Path propagation: weakest link wins (LOW overrules HIGH). */
    public static Confidence minimumAlongPath(List<Confidence> pathConfidences) {
        Confidence worst = Confidence.HIGH;
        for (Confidence c : pathConfidences) {
            if (c.ordinal() > worst.ordinal()) {
                worst = c;
            }
        }
        return worst;
    }

    /** Set variant for convenience. */
    public static Confidence minimumAlongPath(Set<Confidence> pathConfidences) {
        Confidence worst = Confidence.HIGH;
        for (Confidence c : pathConfidences) {
            if (c.ordinal() > worst.ordinal()) {
                worst = c;
            }
        }
        return worst;
    }

    public static Confidence degradeByOneTier(Confidence current) {
        return switch (current) {
            case HIGH -> Confidence.MEDIUM;
            case MEDIUM, LOW -> Confidence.LOW;
        };
    }
}
