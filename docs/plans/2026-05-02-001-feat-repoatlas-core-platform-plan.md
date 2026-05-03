---
title: feat: Build RepoAtlas core platform
type: feat
status: active
date: 2026-05-02
origin: docs/design/REPOATLAS_TDD.md
---

# feat: Build RepoAtlas core platform

## Overview

Build RepoAtlas as a deterministic, local-first Java platform that indexes Spring Boot repositories into immutable SQLite snapshots, serves confidence-aware MCP responses, and enforces architecture and reuse guardrails in CI. The plan prioritizes trustworthiness over breadth: explicit freshness, confidence metadata, and no silent fallback behavior.

---

## Problem Frame

Current assistants repeatedly rediscover architecture, miss Spring and capability semantics, and overstate certainty. RepoAtlas solves this by precomputing repository structure plus capability meaning, then serving narrow, auditable answers with freshness and confidence contracts. This plan targets an MVP that is functionally complete for Java + Spring + Gradle workflows while preserving extensibility for V2 trust additions.

---

## Requirements Trace

- R1. Deterministic local indexing for Java/Spring/Gradle repos with immutable snapshots and atomic publish.
- R2. Freshness-first behavior: every response declares graph freshness and required direct source reads for stale files.
- R3. Confidence/evidence metadata must be attached to graph edges and major recommendation outputs.
- R4. Capability model must be distinct from transport endpoints and support reuse recommendations.
- R5. MCP server must expose task-oriented tools only; no generic SQL surface.
- R6. CLI must support bootstrap/build/status/search/impact/violations/clients/capabilities/review/baseline/ci/mcp.
- R7. CI must block only on high-confidence, strong-evidence findings from the configured blocking set.
- R8. Dirty overlay must be off by default and forbidden for CI, high-confidence recommendations, and architecture enforcement.
- R9. Rebuild orchestration must support full rebuild triggers and bounded incremental rebuilds.
- R10. MVP must be measurable against latency and throughput targets from design docs.

## TDD Section Coverage Matrix (100% Trace)

| TDD section | Coverage in this plan |
|---|---|
| 1. Abstract | Overview + Key Technical Decisions |
| 2. Background and Motivation | Problem Frame |
| 3. Goals and Non-Goals | Requirements Trace + Scope Boundaries |
| 4. Design Principles | Key Technical Decisions + System-Wide Impact invariants |
| 5. System Overview | High-Level Technical Design + U1/U2/U5 |
| 6. Data Model | U2 + U3 |
| 7. Confidence Model | U3 + U5 + U6 + U7 |
| 8. Snapshot and Freshness Subsystem | U2 + U5 + U9 |
| 9. Context Pack Subsystem | U5 |
| 10. Indexing Pipeline | U3 |
| 11. Service Client and Capability Subsystem | U4 |
| 12. Rebuild Orchestration | U2 + U7 |
| 13. Concurrency Model and Parallel Agents | U2 + U7 + Documentation/Operational Notes |
| 14. MCP Server Specification | U5 |
| 15. Architecture Analyzer Suite | U6 |
| 16. CI Gate Model | U7 |
| 17. Configuration | U1 |
| 18. CLI Surface | U1 + U7 + U8 |
| 19. Worked Example A (RBAC reuse) | U4 + U10 E2E scenario |
| 20. Worked Example B (impact flow) | U6 + U10 E2E scenario |
| 21. Performance and Capacity | U8 + U10 |
| 22. Security and Privacy | U9 |
| 23. Failure Modes and Recovery | U9 |
| 24. Testing Strategy | All units test scenarios + U10 |
| 25. Rollout Plan | U11 |
| 26. Alternatives Considered | U12 |
| 27. Glossary | U12 |
| 28. References | Sources & References + U12 |
| 29. Follow ups | Scope Boundaries deferred + U12 |

---

## Scope Boundaries

- Multi-language indexing is out of scope for this plan.
- Hosted remote deployment and multi-tenant service operation are out of scope.
- Agent-generated auto-fixes are out of scope.
- Generic ad hoc SQL query support in MCP is out of scope.
- Rich web UI/dashboard is out of scope.

### Deferred to Follow-Up Work

- Federated cross-repo query mode for enterprise-wide capability lookup: follow-up plan after single-repo core stabilizes.
- HTTP transport for MCP (beyond stdio): follow-up plan once local stdio usage is stable.
- Advanced probabilistic enrichment beyond deterministic extraction + explicit confidence tiers: follow-up plan after baseline trust metrics are healthy.

---

## Context & Research

### Relevant Code and Patterns

- `build.gradle` currently defines a minimal Java app and JUnit harness suitable as the initial scaffold anchor.
- `src/main/java/com/example/app/App.java` and `src/test/java/com/example/app/AppTest.java` provide a baseline smoke-test pattern.
- `docs/design/REPOATLAS_DESIGN.md` defines baseline architecture and operational principles.
- `docs/design/REPOATLAS_TDD.md` is the implementation authority for MVP + V2 trust enhancements.

### Institutional Learnings

- No prior `docs/solutions/` content exists in this repository; this plan becomes the first implementation baseline.

### External References

- None required for initial planning pass; design docs already encode architecture and policy choices.

---

## Key Technical Decisions

- **Single-language Java implementation for indexer + MCP:** avoids cross-language serialization friction on the hottest path.
- **SQLite snapshot store with atomic pointer swap:** maximizes local determinism and rollback safety.
- **Metadata-first MCP responses:** preserves token budget and enforces explicit opt-in for source excerpts.
- **Config-authoritative policy model:** enables team-specific architecture rules without binary changes.
- **No dirty overlay by default:** prevents mixed-truth responses and trust erosion.
- **Trust envelope on critical outputs:** `decision_confidence`, `evidence_quality`, and optional `unsoundness_ledger` make blocking decisions auditable.

---

## Open Questions

### Resolved During Planning

- Should RepoAtlas be a remote service in MVP? **Resolution:** no; local-first stdio model only.
- Should CI block medium-confidence findings? **Resolution:** no; block only on high-confidence + strong evidence.
- Should capability and endpoint be merged in one abstraction? **Resolution:** no; keep separate entities with explicit linkage.

### Deferred to Implementation

- Exact package namespace migration strategy from `com.example.app` to `com.sgarsgaya.codeatlas`: choose final namespace during bootstrap refactor.
- Exact schema migration mechanism versioning format: finalize after initial schema registry implementation.
- Exact analyzer threshold defaults (for god-service and blast-radius warnings): tune after first real repository runs.

---

## Output Structure

    .repoatlas/
      config.yaml
      baseline.json
      graph_status.json
      graph_latest.pointer
      snapshots/
      context_packs/
      reports/
      cache/
    openapi-specs/
    src/main/java/com/sgarsgaya/codeatlas/
      config/
      mappers/
      controllers/
      service/
      service/impl/
      dto/
      repositories/          # JPA repository implementations
      util/
      constants/
      graph/model/
      graph/persist/
      indexer/
      spring/
      openapi/
      capability/
      freshness/
      contextpack/
      mcp/
      analyzer/
      orchestration/
      trust/
      security/
      recovery/
    src/main/resources/
      application.yml
      application-local.yml
    src/test/java/com/sgarsgaya/codeatlas/
      unit/
      integration/

---

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance for review, not implementation specification. The implementing agent should treat it as context, not code to reproduce.*

```text
bootstrap -> full_build -> validate_snapshot -> atomic_publish -> serve_via_mcp

incremental_build:
  detect_changed_files
  compute_affected_subgraph(depth=2 local, 3 ci)
  reindex_subset
  validate_snapshot
  atomic_publish

mcp_request(tool, task_context):
  load_active_snapshot
  evaluate_freshness(relevant_files)
  run_tool_query
  attach_confidence_and_evidence
  if stale: include read_source_directly
  return lean_response
```

---

## E2E Technical Blueprint (Classes, Methods, Libraries, Imports)

This section defines implementation contracts to remove ambiguity. Method names are prescriptive for the first implementation pass and may evolve only with matching test updates.

### Planned Gradle Libraries

- `com.github.javaparser:javaparser-symbol-solver-core` for AST and symbol resolution.
- `org.yaml:snakeyaml` for `.repoatlas/config.yaml` parsing.
- `org.xerial:sqlite-jdbc` for local snapshot persistence.
- `com.fasterxml.jackson.core:jackson-databind` and `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` for JSON/YAML serialization.
- `info.picocli:picocli` for CLI argument parsing.
- `org.slf4j:slf4j-api` and `ch.qos.logback:logback-classic` for structured local logging.
- `org.junit.jupiter:junit-jupiter`, `org.mockito:mockito-core`, `org.assertj:assertj-core` for tests.

### Proposed Core Classes and Methods

- `cli.RepoAtlasCli`
  - `int run(String[] args)`
  - `int bootstrap()`
  - `int build(boolean incremental)`
  - `int status()`
  - `int review()`
  - `int ci()`
- `config.RepoAtlasConfigLoader`
  - `RepoAtlasConfig load(Path configPath)`
  - `RepoAtlasConfig bootstrapDefaults()`
  - `List<String> validate(RepoAtlasConfig config)`
- `graph.persist.SnapshotWriter`
  - `Path writeSnapshot(GraphBatch batch, SnapshotMetadata metadata)`
  - `void writeNodes(Connection conn, List<Node> nodes)`
  - `void writeEdges(Connection conn, List<Edge> edges)`
- `graph.persist.SnapshotValidator`
  - `ValidationResult validate(Path snapshotPath, Optional<Path> previousSnapshot)`
  - `boolean validateSchema(Connection conn)`
  - `boolean validateForeignKeys(Connection conn)`
- `orchestration.AtomicPublisher`
  - `PublishResult publish(Path snapshotPath, PublishContext context)`
  - `void swapPointerAtomically(Path pointerPath, String snapshotId)`
  - `void pruneSnapshots(Path snapshotsDir, int retentionCount)`
- `indexer.RepositoryScanner`
  - `ScanResult scan(Path repoRoot, RepoAtlasConfig config)`
  - `Map<Path, String> fingerprintFiles(List<Path> files)`
- `indexer.JavaAstIndexer`
  - `IndexedSymbols indexJava(ScanResult scanResult)`
  - `List<EdgeCandidate> extractCallEdges(CompilationUnit unit)`
- `spring.SpringEnricher`
  - `SpringGraph enrich(IndexedSymbols symbols)`
  - `List<Edge> resolveBeanInjectionEdges(SpringGraph springGraph)`
- `openapi.OpenApiMapper`
  - `OpenApiGraph map(Path openApiSpec, SpringGraph springGraph)`
  - `MappingDecision mapOperationToController(Operation operation, SpringGraph springGraph)`
- `capability.CapabilityManifestLoader`
  - `CapabilityBundle loadBundle(Path bundlePath)`
  - `ChecksumResult verifyChecksum(Path bundlePath)`
- `capability.ReuseRecommender`
  - `ReuseRecommendation recommend(CapabilityQuery query, CapabilityContext context)`
  - `double scoreCandidate(CapabilityCandidate candidate, RankingWeights weights)`
- `freshness.FreshnessEvaluator`
  - `FreshnessVerdict evaluate(TaskScope scope, SnapshotContext snapshotContext)`
  - `List<Path> findSourceNewerThanGraph(TaskScope scope, SnapshotContext snapshotContext)`
- `contextpack.ContextPackManager`
  - `ContextPack createPack(TaskRequest request, SnapshotContext snapshotContext)`
  - `ContextPackValidation validatePack(ContextPack pack)`
  - `ContextPack refreshPack(ContextPack pack, SnapshotContext snapshotContext)`
- `mcp.RepoAtlasMcpServer`
  - `void start()`
  - `ToolResponse handleToolCall(String toolName, JsonNode input)`
  - `ToolResponse withTrustEnvelope(ToolResponse response, TrustContext trustContext)`
- `analyzer.ArchitectureViolationAnalyzer`
  - `List<Finding> analyze(GraphView graphView, RepoAtlasConfig config)`
- `analyzer.ImpactAnalyzer`
  - `ImpactReport analyzeImpact(ImpactRequest request, GraphView graphView)`
- `analyzer.OpenApiDriftAnalyzer`
  - `List<Finding> analyzeDrift(GraphView graphView, OpenApiGraph openApiGraph)`
- `trust.CiDecisionPolicy`
  - `GateDecision evaluate(Finding finding, CiPolicy policy)`
  - `boolean isBlocking(Finding finding, CiPolicy policy)`
- `orchestration.IncrementalDriftVerifier`
  - `DriftReport compareIncrementalVsFull(Path incrementalSnapshot, Path fullSnapshot)`
  - `DriftSeverity classify(DriftMetrics metrics, DriftThresholds thresholds)`

### Primary Java Imports by Layer

- Core Java: `java.nio.file.*`, `java.sql.*`, `java.time.*`, `java.util.*`, `java.util.concurrent.*`
- JSON/YAML: `com.fasterxml.jackson.databind.*`, `com.fasterxml.jackson.dataformat.yaml.*`, `org.yaml.snakeyaml.*`
- Parsing/indexing: `com.github.javaparser.*`, `com.github.javaparser.ast.*`, `com.github.javaparser.symbolsolver.*`
- CLI/logging: `picocli.CommandLine.*`, `org.slf4j.Logger`, `org.slf4j.LoggerFactory`
- Testing: `org.junit.jupiter.api.*`, `org.mockito.*`, `org.assertj.core.api.Assertions.*`

### E2E Flows (Must Pass)

- Flow E1 (bootstrap): `repoatlas bootstrap` -> config init -> full build -> publish -> MCP install -> health report.
- Flow E2 (reuse): `find_existing_capability` on permission-check task -> `must_reuse` recommendation when authoritative bundle exists.
- Flow E3 (impact): `repoatlas impact <path>` -> entry points + transaction boundaries + risk notes with confidence tags.
- Flow E4 (staleness): modify relevant source file after snapshot -> tool returns `stale_for_relevant_files` + `read_source_directly`.
- Flow E5 (CI): run `repoatlas ci` -> block only if severity + confidence + evidence + no-overlay conditions are all true.

---

## Implementation Units

- [ ] U1. **Bootstrap project structure and configuration authority**

**Goal:** Convert the minimal Java app scaffold into a RepoAtlas-aligned project layout with config-first behavior and foundational CLI entry points.

**Requirements:** R1, R6, R8

**Dependencies:** None

**Files:**
- Modify: `build.gradle`
- Modify: `settings.gradle`
- Create: `src/main/java/com/sgarsgaya/codeatlas/controllers/RepoAtlasCliController.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/RepoAtlasCommandService.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/impl/RepoAtlasCommandServiceImpl.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/config/RepoAtlasConfig.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `.repoatlas/config.yaml`
- Create: `.repoatlas/baseline.json`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/controllers/RepoAtlasCliControllerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/config/RepoAtlasConfigTest.java`

**Approach:**
- Establish package layout and command dispatch skeleton for all required CLI commands.
- Implement config loading with explicit precedence: file values over defaults once file exists.
- Seed baseline file format for future analyzer suppression logic.

**Execution note:** Start with failing config-authority and command-registration tests before implementing command handlers.

**Patterns to follow:**
- `build.gradle`
- `src/test/java/com/example/app/AppTest.java`

**Test scenarios:**
- Happy path: loading `.repoatlas/config.yaml` with valid layer and CI blocks returns parsed authoritative config.
- Edge case: absent config triggers bootstrap defaults only for initialization, then persists generated config.
- Error path: malformed YAML returns explicit config validation error with field path.
- Integration: CLI `bootstrap` writes expected `.repoatlas` scaffold files atomically.

**Verification:**
- Running CLI `bootstrap` creates config + baseline and exits without exceptions.

---

- [ ] U2. **Implement graph schema, snapshot store, and atomic publish**

**Goal:** Build immutable SQLite snapshot persistence with pointer-based atomic activation and validation gates.

**Requirements:** R1, R3, R9

**Dependencies:** U1

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/graph/model/Node.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/graph/model/Edge.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/graph/persist/GraphSchema.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/graph/persist/SnapshotWriter.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/graph/persist/SnapshotValidator.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/orchestration/AtomicPublisher.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/graph/SnapshotWriterTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/graph/SnapshotValidatorTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/orchestration/AtomicPublisherTest.java`

**Approach:**
- Define schema tables for nodes, edges, capabilities, artifacts, and version metadata.
- Enforce required edge metadata at write time (confidence, evidence source, source info, reason, snapshot id).
- Implement publish flow with lock, validate, fsync, pointer swap, status update, retention prune.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: valid snapshot publishes and updates `graph_latest.pointer` to new snapshot id.
- Edge case: publish crash simulation before pointer rename leaves prior pointer intact.
- Error path: schema mismatch causes snapshot rejection and artifact cleanup.
- Integration: retention policy prunes older snapshots while preserving active and rollback snapshots.

**Verification:**
- Snapshot lifecycle tests demonstrate immutable snapshots and atomic active-pointer transitions.

---

- [ ] U3. **Build deterministic indexing pipeline for Java, Spring, and OpenAPI**

**Goal:** Produce graph nodes/edges from repository scan, AST extraction, Spring enrichment, and OpenAPI mapping.

**Requirements:** R1, R2, R3, R9

**Dependencies:** U2

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/indexer/RepositoryScanner.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/indexer/JavaAstIndexer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/spring/SpringEnricher.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/openapi/OpenApiMapper.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/mappers/EdgeMetadataMapper.java`
- Create: `openapi-specs/README.md`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/indexer/RepositoryScannerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/indexer/JavaAstIndexerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/spring/SpringEnricherTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/openapi/OpenApiMapperTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/IndexerPipelineIntegrationTest.java`

**Approach:**
- Implement deterministic scanner honoring `.gitignore`, `.repoatlasignore`, and `generated.paths`.
- Parse Java symbols and calls with confidence tiering.
- Enrich Spring structures and bean injection edges.
- Map OpenAPI operations to controllers using priority chain with ambiguity reporting.

**Execution note:** Implement core indexing behavior test-first for scanner and metadata funnel; use integration tests for end-to-end edge completeness.

**Patterns to follow:**
- `docs/design/REPOATLAS_DESIGN.md`
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: repository with controller/service/repository classes yields expected node and edge taxonomy.
- Edge case: unresolved symbol call captured as low-confidence edge rather than silently dropped.
- Error path: malformed OpenAPI document records mapping diagnostics and degrades confidence.
- Integration: every persisted edge includes all six required metadata fields.

**Verification:**
- Full indexing integration test emits deterministic graph for fixture repo and passes metadata invariants.

---

- [ ] U4. **Implement service client and capability subsystem**

**Goal:** Model business capabilities separately from transport and deliver reuse recommendations.

**Requirements:** R4, R3, R6

**Dependencies:** U3

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/capability/CapabilityManifestLoader.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/capability/ClientArtifactIndexer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/capability/CapabilityMatcher.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/CapabilityService.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/impl/CapabilityServiceImpl.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/capability/CapabilityManifestLoaderTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/capability/CapabilityMatcherTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/service/CapabilityServiceImplTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/CapabilityReuseIntegrationTest.java`

**Approach:**
- Parse and validate capability bundle artifacts with source-of-truth hierarchy.
- Link client methods to capability semantics and transport edges.
- Rank reuse recommendations with confidence, evidence quality, and reuse priority policy.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: manifest-backed client method returns high-confidence `must_reuse` recommendation.
- Edge case: missing manifest falls back to medium-confidence inference path with recommendation caveat.
- Error path: malformed capability bundle fails validation with actionable diagnostics.
- Integration: `find_existing_capability` fixture query returns both semantic and transport linkage without conflation.

**Verification:**
- Capability integration fixtures reproduce the documented RBAC-style reuse workflow.

---

- [ ] U5. **Deliver MCP server, freshness contract, and context packs**

**Goal:** Serve task-oriented graph APIs over stdio with strict freshness and stale-routing behavior.

**Requirements:** R2, R3, R5, R8

**Dependencies:** U2, U3, U4

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/mcp/RepoAtlasMcpServer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/mcp/ToolRegistry.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/freshness/FreshnessEvaluator.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/contextpack/ContextPackManager.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/trust/TrustEnvelopeBuilder.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/mcp/RepoAtlasMcpServerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/freshness/FreshnessEvaluatorTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/contextpack/ContextPackManagerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/McpContractIntegrationTest.java`

**Approach:**
- Implement required tool catalog with strict schemas and lean default payloads.
- Compute freshness against relevant file fingerprints and include `read_source_directly` routing.
- Support context pack pin/validate/refresh lifecycle by task scope.
- Enforce no-overlay default and mark unsafe modes explicitly when opt-in diagnostics are enabled.

**Patterns to follow:**
- `docs/design/REPOATLAS_DESIGN.md`
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: fresh snapshot query returns lean response with empty `read_source_directly`.
- Edge case: changed relevant file returns `stale_for_relevant_files` and targeted source-read instructions.
- Error path: unknown tool invocation returns schema-driven error envelope.
- Integration: trust envelope fields appear on major analysis tools and remain absent for non-critical lightweight calls where configured.

**Verification:**
- MCP integration tests validate tool schemas, freshness contract, and default no-overlay safety behavior.

---

- [ ] U6. **Implement analyzer suite and review workflows**

**Goal:** Produce deterministic architectural analysis outputs and human/machine report artifacts.

**Requirements:** R6, R7, R10

**Dependencies:** U3, U4, U5

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/analyzer/ArchitectureViolationAnalyzer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/analyzer/ImpactAnalyzer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/analyzer/OpenApiDriftAnalyzer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/analyzer/CircularDependencyAnalyzer.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/util/ReportFormatter.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/analyzer/ArchitectureViolationAnalyzerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/analyzer/ImpactAnalyzerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/analyzer/OpenApiDriftAnalyzerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/analyzer/CircularDependencyAnalyzerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/ReviewWorkflowIntegrationTest.java`

**Approach:**
- Implement analyzers listed in design docs with confidence-aware finding records.
- Emit JSON primary outputs plus SARIF and human-readable markdown report formats.
- Ensure every blocking-capable finding carries reproducible evidence fields.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: known layer violation fixture is detected as high-confidence critical finding.
- Edge case: ambiguous call graph emits warning-grade finding with explicit ambiguity reason.
- Error path: missing baseline file in review mode returns non-blocking setup error and recovery hint.
- Integration: `repoatlas review` output includes deterministic finding ordering and stable identifiers across runs.

**Verification:**
- Analyzer outputs are reproducible from snapshot data and satisfy evidence completeness checks.

---

- [ ] U7. **Implement CI gate engine, baseline discipline, and drift verification**

**Goal:** Enforce trustworthy blocking policy and protect incremental correctness with shadow full rebuild checks.

**Requirements:** R7, R8, R9, R10

**Dependencies:** U2, U3, U6

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/controllers/CiCommandController.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/CiDecisionService.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/service/impl/CiDecisionServiceImpl.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/analyzer/BaselineManager.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/orchestration/IncrementalDriftVerifier.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/trust/CiDecisionPolicy.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/controllers/CiCommandControllerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/analyzer/BaselineManagerTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/orchestration/IncrementalDriftVerifierTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/CiGateIntegrationTest.java`

**Approach:**
- Apply blocking policy conjunctively: severity in block set, high decision confidence, strong/authoritative evidence, no overlay usage.
- Implement baseline capture and delta evaluation against legacy findings.
- Add shadow full rebuild diff classification (`critical`/`major`/`minor`) with threshold enforcement hooks.

**Execution note:** Begin with policy contract tests to lock blocking semantics before wiring CLI behavior.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: new high-confidence critical violation over baseline fails CI.
- Edge case: finding with critical severity but medium confidence is downgraded to warning.
- Error path: overlay-enabled analysis input is rejected for CI gating.
- Integration: shadow full rebuild divergence above major threshold forces full rebuild recommendation and non-zero CI exit.

**Verification:**
- CI command behavior matches documented gate model under mixed-confidence and mixed-evidence fixture sets.

---

- [ ] U8. **Performance, operability, and adoption hardening**

**Goal:** Validate operational targets, finalize docs/runbook, and ensure developer onboarding path is reliable.

**Requirements:** R6, R10

**Dependencies:** U1, U2, U3, U4, U5, U6, U7

**Files:**
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/PerformanceSmokeTest.java`
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/BootstrapEndToEndTest.java`
- Create: `docs/design/REPOATLAS_MVP_RUNBOOK.md`
- Modify: `README.md`

**Approach:**
- Add lightweight performance harness for key SLOs (initial build, incremental build, MCP p99 for representative tools).
- Validate one-command bootstrap onboarding with deterministic outputs.
- Publish operator/developer runbook covering rebuild triggers, stale handling, and CI interpretation.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`
- `docs/design/REPOATLAS_DESIGN.md`

**Test scenarios:**
- Happy path: bootstrap flow initializes config, builds graph, installs MCP, and reports health summary.
- Edge case: low-resource environment triggers graceful degraded warnings without corrupting active snapshot.
- Error path: invalid config policy key fails fast with clear remediation guidance.
- Integration: performance smoke test verifies target guardrails or emits explicit non-blocking deviation report.

**Verification:**
- Team can run bootstrap and first CI pass end-to-end without manual patching.

---

- [ ] U9. **Security, privacy, and failure-recovery hardening**

**Goal:** Implement explicit controls and recovery behavior for TDD security/privacy and failure-mode sections.

**Requirements:** R2, R5, R7, R8, R9

**Dependencies:** U2, U4, U5, U7

**Files:**
- Create: `src/main/java/com/sgarsgaya/codeatlas/security/SourcePrivacyGuard.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/security/BundleTrustVerifier.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/recovery/RecoveryCoordinator.java`
- Create: `src/main/java/com/sgarsgaya/codeatlas/mcp/ErrorEnvelopeFactory.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/security/SourcePrivacyGuardTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/security/BundleTrustVerifierTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/unit/recovery/RecoveryCoordinatorTest.java`
- Test: `src/test/java/com/sgarsgaya/codeatlas/integration/FailureModeIntegrationTest.java`

**Approach:**
- Enforce source excerpt opt-in policy and report storage rules.
- Validate bundle checksums and capability id conflicts with deterministic conflict handling.
- Implement startup and runtime recovery policies for pointer corruption, missing snapshots, parse failures, and stdio framing failures.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: local processing path emits no remote transport dependencies and writes reports only to `.repoatlas/reports/`.
- Edge case: bundle checksum mismatch downgrades capability path and emits explicit warning.
- Error path: snapshot pointer corruption falls back to last valid snapshot and records incident.
- Integration: MCP tool error always returns `error_code` and `error_reason` envelope.

**Verification:**
- Security and failure behavior is deterministic and auditable under fault injection.

---

- [ ] U10. **E2E conformance and trust-quality targets**

**Goal:** Convert TDD performance and trust-quality targets into executable end-to-end assertions.

**Requirements:** R2, R3, R4, R6, R10

**Dependencies:** U3, U4, U5, U6, U7, U8, U9

**Files:**
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/E2EReuseFlowTest.java`
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/E2EImpactFlowTest.java`
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/E2EFreshnessRoutingTest.java`
- Create: `src/test/java/com/sgarsgaya/codeatlas/integration/E2ETrustMetricsTest.java`
- Create: `src/test/resources/fixtures/pilot-service/`

**Approach:**
- Build deterministic fixture services for reuse and impact workflows.
- Assert SLO/SLA targets (build latency, incremental latency, MCP p99 guardrails) and trust-quality targets.
- Verify trust envelope coverage percentage and stale-routing completeness.

**Execution note:** Start with deterministic fixture data; then tune performance harness to reduce flakiness.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Happy path: RBAC reuse query returns ranked recommendation with rationale and expected confidence/evidence.
- Edge case: stale relevant file always appears in `read_source_directly` with degraded confidence.
- Error path: unsoundness-ledger generation failure caps decision confidence to medium and emits warning.
- Integration: shadow full-vs-incremental drift metrics satisfy critical==0 and major<=2%.

**Verification:**
- E2E test suite proves documented examples and trust targets as executable contracts.

---

- [ ] U11. **Rollout phases and operational adoption gates**

**Goal:** Encode pilot, beta, and org rollout policy as explicit delivery gates.

**Requirements:** R6, R7, R10

**Dependencies:** U8, U10

**Files:**
- Create: `docs/design/REPOATLAS_ROLLOUT_PLAN.md`
- Create: `docs/design/REPOATLAS_ACCEPTANCE_GATES.md`
- Modify: `README.md`

**Approach:**
- Define phase exit criteria, service count targets, and CI policy hardening sequence.
- Specify when capability manifests transition from optional to required for `must_reuse`.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`

**Test scenarios:**
- Test expectation: none -- documentation and rollout governance only.

**Verification:**
- Rollout decisions are codified and reviewable before broad adoption.

---

- [ ] U12. **Design ledger parity documentation (alternatives, glossary, references, follow-ups)**

**Goal:** Preserve non-code design context from TDD as first-class implementation documentation.

**Requirements:** R1, R6

**Dependencies:** U11

**Files:**
- Create: `docs/design/REPOATLAS_ALTERNATIVES_LEDGER.md`
- Create: `docs/design/REPOATLAS_GLOSSARY.md`
- Create: `docs/design/REPOATLAS_FOLLOWUPS.md`
- Modify: `docs/design/REPOATLAS_MVP_RUNBOOK.md`

**Approach:**
- Extract and adapt alternatives/follow-up decisions into living docs tied to implementation state.
- Keep glossary synchronized with code-level terminology used by CLI, MCP, and analyzers.

**Patterns to follow:**
- `docs/design/REPOATLAS_TDD.md`
- `docs/design/REPOATLAS_DESIGN.md`

**Test scenarios:**
- Test expectation: none -- documentation parity and governance artifact updates.

**Verification:**
- Repo contains a complete operational record of the design rationale and next-step backlog.

---

## System-Wide Impact

- **Interaction graph:** CLI orchestration becomes the control plane for indexing, MCP serving, analyzers, and CI policy decisions.
- **Error propagation:** Snapshot validation/publish failures must remain local and never corrupt the currently active pointer target.
- **State lifecycle risks:** Snapshot retention and context pack invalidation must avoid orphaned references after rebuild/publish.
- **API surface parity:** CLI, MCP tool schemas, and CI output envelopes must stay semantically aligned on confidence/evidence vocabulary.
- **Integration coverage:** End-to-end tests are mandatory for bootstrap, indexing, stale routing, analyzer findings, and CI gate decisions.
- **Unchanged invariants:** Live source remains authoritative over stale graph data; dirty overlay remains non-default and non-CI-safe.

---

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Parser/solver fidelity gaps on complex Spring code reduce confidence too often | Build fixture corpus early and tune resolver + ambiguity policy with explicit diagnostics |
| Incremental rebuild correctness drifts from full rebuild over time | Enforce scheduled shadow full rebuild diff and threshold-based fallback |
| CI false positives reduce trust and adoption | Keep strict blocking conjunction and require explicit evidence fields for blockers |
| Capability bundle quality varies by owning teams | Provide schema validation and degrade recommendation confidence instead of over-asserting |
| Performance regressions as analyzer set expands | Add performance smoke tests and bounded response row caps per tool |

---

## Documentation / Operational Notes

- Document local `.repoatlas/` lifecycle and which files are committed vs machine-local.
- Publish troubleshooting for stale responses (`read_source_directly`) and when to force full rebuild.
- Add onboarding notes for parallel worktrees and single-writer/multi-reader behavior.
- Define release checklist for schema version bumps and migration-safe fallback behavior.

---

## Sources & References

- Origin document: `docs/design/REPOATLAS_TDD.md`
- Companion design: `docs/design/REPOATLAS_DESIGN.md`
- Additional context: `docs/design/REPOATLAS_TDD_V2.md`
- Tradeoff ledger: `docs/design/REPOATLAS_TRADEOFFS_AND_SIDE_EFFECTS.md`
- JavaParser Symbol Solver docs: [https://javaparser.org](https://javaparser.org)
- SQLite docs: [https://www.sqlite.org/docs.html](https://www.sqlite.org/docs.html)
- OpenAPI specification: [https://spec.openapis.org/oas/latest.html](https://spec.openapis.org/oas/latest.html)
- Model Context Protocol docs: [https://modelcontextprotocol.io](https://modelcontextprotocol.io)
