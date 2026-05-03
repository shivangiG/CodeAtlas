# RepoAtlas Research Synthesis and Design Hardening Plan

Status: Draft  
Purpose: Convert the 50-paper corpus into concrete RepoAtlas design guidance  
Method: Mixed evidence mode (full text where available, abstract/metadata where paywalled), with explicit confidence tagging

---

## 1) Scope and Method

This document synthesizes the provided 50-paper list into:

- a corpus map by theme,
- validated findings and their evidence strength,
- concrete problems in the current RepoAtlas direction,
- suggested solutions,
- and a prioritized roadmap ranked by effectiveness first, then simplicity.

### Evidence depth legend

- **F (High-confidence input):** full text reviewed
- **A (Medium-confidence input):** abstract/metadata reviewed (often paywalled)
- **T (Low-confidence input):** title/venue-level inference only

Any claim backed mainly by paywalled abstracts is marked as partial-confidence.

---

## 2) Corpus Map by Theme

## A. Graph-backed code retrieval
Papers: **1, 2, 3, 4, 5, 6, 7, 8**  
Evidence depth: F-heavy with some A  
Core question: does graph-first retrieval outperform repeated file scans for repository tasks?

## B. Java/Spring call graph and static analysis
Papers: **9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20**  
Evidence depth: mixed; strong full-text support on reflection/unsoundness, partial on some tool papers  
Core question: how far static analysis can be trusted in Spring-heavy Java systems.

## C. Architecture recovery and conformance
Papers: **21, 22, 23, 24, 25, 26, 27, 28, 29, 30**  
Evidence depth: mixed, paywall-heavy in comparative studies  
Core question: recovering architecture from code/dependencies and validating conformance.

## D. Change impact analysis and slicing
Papers: **31, 32, 33, 34, 35, 36**  
Evidence depth: foundational F + microservice CIA partly paywalled  
Core question: how to propagate change impact beyond changed files.

## E. Incremental build and incremental static analysis
Papers: **37, 38, 39, 40, 41**  
Evidence depth: strong F  
Core question: sound incremental recomputation and dependency error detection.

## F. API usage recommendation and reuse
Papers: **42, 43, 44, 45, 46, 47**  
Evidence depth: mixed  
Core question: recommending reusable APIs/capabilities from code patterns and examples.

## G. Microservice API evolution and reusability
Papers: **48, 49, 50**  
Evidence depth: mixed  
Core question: API evolution risk and service reusability constraints in microservice ecosystems.

---

## 3) What Is Strongly Validated vs Partially Validated

## 3.1 Strongly validated (high confidence)

- **Graph-first retrieval is superior to repeated ad-hoc repository exploration** for many coding tasks, provided retrieval is structural and task-aware.
- **Java/Spring-aware analysis is mandatory** in Spring-heavy systems; generic parsing is insufficient.
- **Call graph unsoundness is real and recurrent** due to reflection, framework wiring, proxies, and configuration interactions.
- **Incremental dependency-driven recomputation is the right execution model** when paired with soundness controls.
- **Impact analysis requires graph reachability**, not changed-file-only heuristics.

## 3.2 Partially validated (medium/low confidence due to access limits)

- Comparative ranking of modern architecture recovery tools in microservices.
- Comparative ranking of some Java static-analysis frameworks where full paper access was limited.
- Current-generation API recommendation efficacy in modern LLM workflows.
- Detailed, quantitative API evolution mitigation strategies from paywalled studies.

Implication: these areas should remain configurable/experimental in RepoAtlas until full-text review closes evidence gaps.

---

## 4) Validation Map for RepoAtlas Design Decisions

| RepoAtlas decision | Validation status | Notes |
|---|---|---|
| Graph-backed retrieval over repeated grep/file reads | Strong | Supported by graph retrieval corpus and RAG-for-code trends |
| Java/Spring-aware static analysis | Strong | Reflection/IoC evidence makes this non-optional |
| Confidence + evidence metadata on graph facts | Strong | Required to handle unsoundness honestly |
| Immutable snapshots + freshness validation | Strong | Consistent with incremental build and reproducibility literature |
| Config-driven architecture conformance | Medium-strong | Supported by conformance studies; comparative tooling evidence partly paywalled |
| Reachability + bounded expansion for impact | Medium-strong | Supported by slicing/CIA foundations and microservice CIA direction |
| Capability reuse recommendation (not just path matching) | Medium | Strong conceptual support; empirical calibration still needed |
| Manifests as human-reviewable SoT, SQLite as generated cache | Medium-strong | Architecturally consistent and governance-friendly; not always explicitly prescribed in papers |

---

## 5) Problems in Current RepoAtlas Direction

## P1. Confidence is present but not calibrated for derived conclusions

Current risk: edge-level confidence exists, but output-level confidence (e.g., final impact recommendation) can still appear overconfident.

## P2. Unsoundness is acknowledged but not operationalized as a first-class output

Current risk: reflection/proxy ambiguity may be known internally but not visible enough in user-facing responses.

## P3. Incremental correctness is policy-driven, not continuously verified against full rebuild truth

Current risk: drift can accumulate without explicit sampled equivalence checks.

## P4. Capability reuse recommendations are discoverable but under-specified as ranking decisions

Current risk: recommendations degrade to “candidate list” quality instead of actionable “best next call.”

## P5. Architecture conformance is mostly static and can miss runtime-induced divergence

Current risk: false confidence in static conformance where runtime wiring/profile conditions change behavior.

## P6. API evolution is not deeply integrated into impact and reuse output

Current risk: recommendation may suggest technically available but evolution-risky APIs.

## P7. Evidence provenance is recorded but not quality-scored separately from confidence

Current risk: users conflate confidence with evidence strength and cannot audit trust as precisely as needed.

---

## 6) Suggested Solutions

## S1. Add output-level confidence calibration

Compute confidence not just per edge but per final answer.

- Inputs: edge confidence distribution, ambiguity count, stale-file flags, evidence-source mix.
- Output: calibrated `decision_confidence` + reason vector.

Expected effect: fewer overconfident recommendations.

## S2. Add an unsoundness ledger to responses

Expose unresolved dynamic zones explicitly:

- reflection hotspots,
- dynamic proxy boundaries,
- ambiguous bean injections,
- profile-conditional branches.

Expected effect: transparency and higher reviewer trust.

## S3. Add sampled shadow full rebuild verification

Periodically run full rebuilds and diff against incremental outputs.

- Compare nodes, edges, and tool-level outputs (impact/violations/context).
- Classify divergences as acceptable vs critical.

Expected effect: incremental correctness guarantees grounded in measured reality.

## S4. Formalize capability recommendation as a ranking pipeline

For `find_existing_capability`:

1. candidate generation (manifest + graph + callsite history),
2. scoring (confidence, evidence quality, side-effect fit, must_reuse policy, dependency availability),
3. tie-breakers (owner/stability/version risk).

Expected effect: actionable recommendations and less duplicate integration logic.

## S5. Introduce hybrid static+runtime conformance mode (hardening phase)

Keep static default; add optional runtime corroboration path for disputed findings.

Expected effect: fewer false disputes on conformance in dynamic environments.

## S6. Introduce API evolution risk model

Track operation lineage and consumer exposure:

- removals/renames,
- request/response contract changes,
- deprecation windows,
- consumer dependency map.

Expected effect: safer reuse and impact decisions across service versions.

## S7. Separate evidence quality from confidence

Add `evidence_quality` alongside confidence:

- `authoritative`, `strong`, `inferred`, `weak`.

Expected effect: better auditability and triage quality.

---

## 7) Prioritized Roadmap (Effectiveness First, Simplicity Second)

1. **S3: sampled full-vs-incremental verification**  
   Highest trust gain; prevents silent drift.

2. **S1: output-level confidence calibration**  
   High impact with moderate implementation complexity.

3. **S4: ranked capability recommendation pipeline**  
   High practical value for reuse and duplicate prevention.

4. **S2: unsoundness ledger**  
   Strong trust gain; relatively straightforward once response contracts are updated.

5. **S7: evidence quality dimension**  
   Significant explainability gain with manageable complexity.

6. **S6: API evolution risk model**  
   Important for microservice ecosystems; moderate-to-high effort.

7. **S5: hybrid static+runtime conformance**  
   High potential, but most operational complexity; best in hardening phase.

---

## 8) How to Apply These Learnings While Building RepoAtlas

Use this as a build protocol, not only a literature summary.

## 8.1 Gate-based implementation

- **Gate A (retrieval):** graph-first context tools return smaller, higher-relevance file sets than baseline search.
- **Gate B (semantic extraction):** Java/Spring facts include explicit confidence and evidence provenance.
- **Gate C (incremental correctness):** sampled full-vs-incremental equivalence checks stay within tolerance.
- **Gate D (impact quality):** impact precision at top-k improves vs changed-file-only baseline.
- **Gate E (reuse quality):** capability recommender has measurable acceptance and reduced duplicate call patterns.

## 8.2 Suggested acceptance criteria

- >= 95% of user-visible claims include confidence + evidence source.
- `read_source_directly` always present when relevant files are newer than snapshot.
- Full-vs-incremental divergence on critical outputs below configured threshold.
- `find_existing_capability` returns ranked results with score breakdown and rationale.
- CI blocks only high-confidence critical findings; medium/low remain warnings.

## 8.3 Suggested metrics dashboard

- edge confidence distribution by analyzer/tool
- output-level confidence distribution
- full-vs-incremental divergence rate
- impact precision@k and recall@k
- recommendation acceptance rate
- duplicate direct-call prevention count
- stale-response frequency and routing compliance
- architecture violation trend by severity

---

## 9) Notes on Evidence Limits

Some architecture recovery, API recommendation, and evolution papers in the corpus are paywalled or only partially accessible. Their conclusions are included with reduced confidence where appropriate. Before freezing long-term defaults in those areas, run a full-text deep review cycle and update this synthesis with stronger evidence tags.

---

## 10) Immediate Next Actions for RepoAtlas

1. Implement S3 and S1 as the first hardening tranche.
2. Extend MCP response contracts with unsoundness and evidence-quality fields.
3. Convert `find_existing_capability` from listing to ranking.
4. Add “Research-to-decision” traceability tags in the TDD for every major subsystem.
5. Schedule a paywall-closure review sprint for partial-confidence areas.

This sequence delivers the fastest trust improvement with manageable complexity.
