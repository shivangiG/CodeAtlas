# RepoAtlas: Trade-offs, Side Effects, and Residual Risks

Status: Draft for technical review
Audience: Engineering review and conference deep dive
Companion document to: `REPOATLAS_DESIGN.md`

This document catalogues the operational consequences of every locked decision. It is intentionally separate from the main design document so that a reviewer can read what the system is, then read what the system costs, without confusion.

For each decision, the catalogue lists the trade-off, when it manifests, the mitigation in place, the residual risk after mitigation, and the operational signal that should detect the failure mode if it occurs.

---

## 1. How to Read This Document

Each entry follows the same shape:

- **Decision**: short identifier matching the main design document section.
- **Trade-off**: what we paid for the choice.
- **When it manifests**: concrete situations where the cost shows up.
- **Mitigation**: the operational or design control that bounds the cost.
- **Residual risk**: what remains after mitigation.
- **Detection signal**: how an operator or CI run will see the issue if it happens.

The catalogue is exhaustive over the Decision Ledger from the main design document. It does not duplicate the rationale; it documents the costs the rationale already accepts.

---

## 2. Decision-by-Decision Side Effect Catalogue

### 2.1 Symbol Identity (Hybrid)

- **Trade-off**: two coexisting identity strategies create promotion and demotion events. Tooling has to handle key transitions when an unresolved symbol becomes resolved.
- **When it manifests**: large refactors that change visibility, generic parameters, or method overloads; first-time addition of source jars that promote previously fallback-keyed symbols.
- **Mitigation**: every promotion and demotion is recorded as a resolution event in `graph_status.json`. Edges carry both keys during transition.
- **Residual risk**: dashboards or analyzers that join on a single key may double-count during the transition window.
- **Detection signal**: spike in `resolution_events.promotions` or `demotions` between adjacent snapshots; assertion in validation that no edge references a stale key after publish.

### 2.2 Storage Topology (Per-Repo plus Federation)

- **Trade-off**: cross-service questions require a federation layer. Aggregation has to choose between freshness across repos.
- **When it manifests**: blast radius questions that span service A and service B; capability lookups that depend on the consumer-side cache being current with the provider-side bundle.
- **Mitigation**: federated queries are best-effort, never authoritative. Each result tags the source repo and source snapshot id. Bundle freshness is included in `graph_status` for capability questions.
- **Residual risk**: a federated answer may mix snapshots taken at different wall-clock times. Engineers must read the per-repo timestamps to interpret the result.
- **Detection signal**: `federation.snapshot_skew_ms` exceeds a configured threshold; warning attached to the response.

### 2.3 Call Resolution (Best-Effort with Confidence Tiers)

- **Trade-off**: the graph contains medium and low confidence edges that downstream code must interpret carefully.
- **When it manifests**: dynamic proxies, reflection, generated code, and Spring autowiring without explicit types.
- **Mitigation**: confidence propagation rules; analyzers consult confidence; CI blocks only on high confidence; impact analysis treats low confidence as a hint.
- **Residual risk**: a developer who reads an unfiltered edge dump may treat low-confidence edges as facts.
- **Detection signal**: per-tool unit tests assert that responses never elevate medium or low edges above their level.

### 2.4 Context Slice Objective (Precision-First)

- **Trade-off**: small slices may exclude relevant files for unusual tasks.
- **When it manifests**: cross-cutting refactors, framework upgrades, or new capability rollouts that touch many otherwise-unrelated modules.
- **Mitigation**: explicit `expand` mode; `minimal_context_for_task` exposes a `why` field so the agent can ask for more if the listed files do not justify themselves.
- **Residual risk**: a junior engineer or unfamiliar agent may not know to expand and may miss a file.
- **Detection signal**: telemetry on `expand_used` rate; if it stays at zero, the default cap may be too tight.

### 2.5 Rule Authority (Config-Authoritative)

- **Trade-off**: the tool ships with no enforced opinion. Teams must define their own layer rules to get value.
- **When it manifests**: first-time bootstrap on a new repo with no rules.
- **Mitigation**: bootstrap writes a sane default `config.yaml` and prints a one-line summary of what would be enforced.
- **Residual risk**: teams that never edit the default config get a generic enforcement that may not match their architecture.
- **Detection signal**: `config.is_default = true` on the report; review checks during onboarding.

### 2.6 OpenAPI Ambiguity (Top Candidate plus Threshold)

- **Trade-off**: a real ambiguity may be hidden if the threshold is loose; a real signal may be blocked if the threshold is tight.
- **When it manifests**: large services with overlapping route patterns or repeated operationIds across versions.
- **Mitigation**: `ambiguity.ci_threshold_percent` exposed in config; trend tracking with baseline.
- **Residual risk**: gradual drift past the threshold without a clear single PR responsible.
- **Detection signal**: ambiguity rate trend graph; alert on rate-of-change rather than instantaneous level.

### 2.7 Incremental Rebuild

- **Trade-off**: incremental rebuilds can be wrong if the dependency fan-out is too shallow.
- **When it manifests**: indirect impacts beyond depth 2 locally; reflection-driven impact paths; aspect-oriented advice not visible in syntax.
- **Mitigation**: foundation-change full rebuild triggers; CI uses depth 3; periodic full rebuilds on a clock or on operator command.
- **Residual risk**: a niche path of impact escapes incremental detection between full rebuilds.
- **Detection signal**: comparison job that occasionally runs full rebuild and diffs the resulting graph; alerts on nontrivial divergence.

### 2.8 MCP Transport (stdio MVP)

- **Trade-off**: stdio is per-client. Multiple clients require multiple subprocesses.
- **When it manifests**: a developer running several agent assistants simultaneously.
- **Mitigation**: each subprocess reads the same active snapshot. Cost is small because readers are cheap.
- **Residual risk**: subprocess startup latency on first call.
- **Detection signal**: `mcp.startup_ms` metric.

### 2.9 MCP Language (All-Java)

- **Trade-off**: all-Java reduces flexibility for teams that prefer TypeScript-based MCP tooling.
- **When it manifests**: contributors more comfortable in TS who want to extend MCP tools.
- **Mitigation**: tool catalog is fixed in MVP, so language choice is mostly invisible to consumers; future extension surface can be split out if demand warrants.
- **Residual risk**: contributor pool is narrower in the short term.
- **Detection signal**: contributor signal from internal pull requests and issue volume.

### 2.10 Response Safety (Metadata-First)

- **Trade-off**: agents that benefit from inline snippets must opt in, which costs one extra round-trip or one extra parameter.
- **When it manifests**: deep-dive code-walk requests.
- **Mitigation**: `include_source` and `max_lines_per_symbol` parameters; tool documentation makes the opt-in obvious.
- **Residual risk**: agents that fail to opt in produce bullet-point answers when the human wanted a snippet.
- **Detection signal**: agent telemetry of `include_source` adoption.

### 2.11 Response Profile (Decision plus Evidence)

- **Trade-off**: every response carries some evidence overhead.
- **When it manifests**: extremely simple lookups where evidence is irrelevant.
- **Mitigation**: evidence section is bounded; engineers prefer evidence over opacity.
- **Residual risk**: marginal token overhead in micro-tasks.
- **Detection signal**: response size distribution; ensure p99 stays inside cap.

### 2.12 Thresholds (Conservative Defaults)

- **Trade-off**: real findings may be missed when defaults are too lenient.
- **When it manifests**: codebases with high noise levels where conservative defaults find nothing useful.
- **Mitigation**: explicit per-repo overrides; baseline tracking with drift alerts.
- **Residual risk**: teams that never tune thresholds may underuse the tool.
- **Detection signal**: number of findings per repo; flag repos with persistent zero findings for review.

### 2.13 Reports (JSON plus SARIF)

- **Trade-off**: two formats to maintain.
- **When it manifests**: report schema evolution.
- **Mitigation**: shared serialization layer; SARIF emitted from the same internal model.
- **Residual risk**: schema drift between formats.
- **Detection signal**: golden tests on both outputs.

### 2.14 Schema Evolution (Non-Blocking Auto Rebuild)

- **Trade-off**: developers may briefly see results derived from the previous snapshot during a rebuild.
- **When it manifests**: indexer upgrades; schema migrations.
- **Mitigation**: every response shows `graph_status` and `snapshot_id`; freshness messaging makes the situation explicit.
- **Residual risk**: a developer who ignores the `graph_status` field.
- **Detection signal**: anomaly in the agreed-upon distribution between `fresh` and `stale_for_relevant_files`.

### 2.15 Concurrency Model (Single Writer, Many Readers)

- **Trade-off**: only one rebuild can run at a time per repo.
- **When it manifests**: a developer who triggers `build` while one is already running.
- **Mitigation**: lock detection returns immediately with the active job id; the second invocation does not queue.
- **Residual risk**: rebuild queueing semantics may surprise users who expect retries.
- **Detection signal**: `rebuild.lock_held_returns` metric.

### 2.16 Same-File Parallel Edits (Warn, Degrade, Suggest Worktree Split)

- **Trade-off**: agents can still produce conflicting work.
- **When it manifests**: two agents editing the same file in the same worktree.
- **Mitigation**: response carries a conflict warning, lower confidence, and a worktree split recommendation.
- **Residual risk**: human-driven merge issues remain a human problem.
- **Detection signal**: agent telemetry on conflict warnings.

### 2.17 Dirty Overlay (Off by Default; Opt-In Diagnostic Only)

- **Trade-off**: the agent does not get pre-merged context for in-flight edits.
- **When it manifests**: an agent in the middle of a rapid edit-then-ask loop.
- **Mitigation**: agent receives `read_source_directly` with the exact file list; the round-trip cost is small.
- **Residual risk**: agent harnesses that do not honor `read_source_directly` will produce stale answers; this is a harness bug, not a RepoAtlas bug.
- **Detection signal**: telemetry on `read_source_directly` populated but agent did not subsequently read those files; surface as integration warning.

### 2.18 Freshness Metadata (Lean by Default)

- **Trade-off**: lean metadata may not be enough for deep diagnostics.
- **When it manifests**: rebuild correctness investigations.
- **Mitigation**: opt-in verbose diagnostics surface dirty file lists, rebuild state, and snapshot history.
- **Residual risk**: an operator may forget to enable verbose mode during an incident.
- **Detection signal**: incident playbook explicitly references verbose mode.

### 2.19 CI Gates (Critical Only)

- **Trade-off**: real but low-confidence problems do not block.
- **When it manifests**: emerging architecture decay that has not yet produced a high-confidence finding.
- **Mitigation**: warnings tracked over time; trend reports show drift.
- **Residual risk**: gradual decay below the blocking line.
- **Detection signal**: trend reports on warnings; SLO on warning growth rate.

### 2.20 Bootstrap (Full Bootstrap)

- **Trade-off**: first run takes longer.
- **When it manifests**: first-time onboarding on a large monorepo.
- **Mitigation**: bootstrap is interruptible; it logs progress; partial progress survives.
- **Residual risk**: a developer kills bootstrap halfway and is left with a partial state.
- **Detection signal**: `graph_status.json` shows incomplete bootstrap; CLI prints recovery hint on next invocation.

### 2.21 Client Capability Scope (Full)

- **Trade-off**: raw HTTP inference can produce noisy edges in legacy code.
- **When it manifests**: services with bespoke OkHttp wrappers and non-standard URL constants.
- **Mitigation**: confidence tiers for inferred edges; manual overrides; manifest priority chain.
- **Residual risk**: low-confidence edges in dashboards may need filtering by users.
- **Detection signal**: ratio of low-confidence client edges to high-confidence client edges per repo.

### 2.22 Manifest Policy (Optional Now, Required Later)

- **Trade-off**: until manifests are required, recommendations carry medium confidence in many cases.
- **When it manifests**: on every reuse query in a manifest-less environment.
- **Mitigation**: explicit messaging in responses suggesting that owners add manifests.
- **Residual risk**: organizations that never adopt manifests cap their high-confidence reuse benefits.
- **Detection signal**: percentage of reuse recommendations marked `medium` due to missing manifests.

### 2.23 Artifact Source (Local Cache First)

- **Trade-off**: stale local cache can produce misleading client metadata.
- **When it manifests**: dependency upgrades on the consumer side without `gradle --refresh-dependencies`.
- **Mitigation**: bootstrap and `repoatlas clients` print the cache provenance and version of each indexed client.
- **Residual risk**: developers ignore the provenance display.
- **Detection signal**: telemetry showing client-version mismatch between expected and observed.

### 2.24 Capability Naming (Layered Model)

- **Trade-off**: capability names are an editorial concern that requires governance.
- **When it manifests**: capability conflicts between bundles, drift between display names and ids.
- **Mitigation**: capability manifest schema enforces id format; manual overrides record provenance and owner.
- **Residual risk**: organic naming drift across teams.
- **Detection signal**: linter that detects duplicate capability ids across bundles.

### 2.25 MCP Query Surface (Fixed Tools Only)

- **Trade-off**: unusual queries cannot be expressed without a new tool.
- **When it manifests**: research questions that the tool catalog did not anticipate.
- **Mitigation**: tool catalog evolves with usage; a guarded read-only generic tool is permitted later if demand justifies it.
- **Residual risk**: teams may attempt to extract data through tool combinations less efficient than a direct query.
- **Detection signal**: tool call patterns that look like compensating for missing tooling.

### 2.26 SQLite Governance (Cache Only)

- **Trade-off**: capability metadata governance lives in `capabilities.yaml` and the bundle artifact, not in the consumer.
- **When it manifests**: a team that owns a service forgets to publish a refreshed bundle.
- **Mitigation**: provider CI publishes on tag; consumer cache shows last refresh timestamp and warns when older than a configured age.
- **Residual risk**: a stale provider bundle silently degrades consumer reuse confidence.
- **Detection signal**: bundle freshness widget on consumer dashboards.

### 2.27 Live Source Authority

- **Trade-off**: every freshness disagreement degrades to a navigation-only answer plus `read_source_directly`.
- **When it manifests**: agent edit-then-ask loops; long sessions across multiple rebuilds.
- **Mitigation**: lean metadata is built for this case; agent harnesses receive the exact files to read.
- **Residual risk**: agents that ignore the routing cost the developer time.
- **Detection signal**: harness-level telemetry on routing adoption.

---

## 3. Cross-Cutting Risks

### 3.1 Confidence Misuse

The confidence vocabulary is a small surface (`high`, `medium`, `low`). The cross-cutting risk is that callers reduce a multi-dimensional notion of trust to a single tier and miss nuance. Mitigation: confidence travels with `evidence_source` and `reason`. Reviewers can read both.

### 3.2 Snapshot Disk Footprint

Multiple retained snapshots cost disk space. On a monorepo this can become meaningful. Mitigation: configurable retention count; pruning runs in the same lock window as publish.

### 3.3 Manifest Trust Chain

When manifests become required, the trust chain depends on artifact provenance. Mitigation: future signing and verification work is tracked in the open questions section of the design document.

### 3.4 Capability Conflicts

If two providers publish capabilities with the same id, consumers must pick one. Mitigation: capability id includes a domain prefix; conflicting ids fail consumer cache load with a clear error.

### 3.5 Federation Skew

Federated queries assemble per-repo snapshots that may have been taken at different times. Mitigation: each federation answer reports per-repo snapshot id and timestamp.

### 3.6 Agent Harness Compliance

The lean response contract requires that the agent honor `read_source_directly`. Non-compliant harnesses can produce stale answers. Mitigation: this is treated as an integration bug with the harness; RepoAtlas exposes telemetry hooks to detect non-compliance.

---

## 4. Failure Modes by Subsystem

### 4.1 Indexer

- Java parse failures on malformed source: skip the file, log, mark as fallback identity for any partial extraction.
- Symbol resolution failures: degrade affected edges to medium or low; never elevate.
- OpenAPI parser failures: skip the spec, surface in `graph_status.json`, do not block the build.

### 4.2 Snapshot Store

- Disk full: discard new snapshot, keep current pointer.
- Pointer corruption: detect on startup, fall back to last valid snapshot, record incident.
- Validation failure: discard, log, retry on next trigger.

### 4.3 MCP Server

- Snapshot not found: return `graph_status: no_graph` and instruct the agent to run bootstrap.
- Tool error: return error envelope with `error_code`, `error_reason`, never silent failure.
- Stdio framing error: terminate cleanly; harness restarts.

### 4.4 Capability Subsystem

- Missing manifest: degrade to inference, surface suggestion.
- Bundle checksum mismatch: refuse to load, log, surface as warning.
- Conflicting capability id: refuse to load both sources for that id, surface conflict, prefer the manifest with explicit override or domain prefix.

### 4.5 CI Pipeline

- Graph build failure: block.
- Critical high-confidence violation: block.
- Overlay attempted: block (`reject_overlay: true`).
- Warning growth: report, do not block.

---

## 5. Operational Signals to Track

- Indexing duration p50, p95, p99 per repo size class.
- Incremental rebuild duration distribution.
- Snapshot publish success rate.
- MCP response size distribution.
- Confidence distribution per analyzer per repo.
- `read_source_directly` rate per session.
- Bundle freshness across consumer repos.
- Federation snapshot skew distribution.
- Tool error rate by tool name.
- CI blocking rate vs warning rate.
- Adoption of `expand` mode.
- Adoption of `include_source` mode.

These signals are sufficient to detect every failure mode listed in this document.

---

## 6. Risk Register Summary

The headline residual risks after mitigation are:

- Confidence misinterpretation by downstream tools or human readers.
- Manifest gaps producing systematically medium-confidence reuse recommendations.
- Federation snapshot skew during cross-repo investigations.
- Slow gradual architectural decay below the high-confidence blocking line.
- Stale local artifact cache producing misleading client metadata.

Each risk has a detection signal and an operational owner mapping defined in the corresponding subsystem section. None of these risks invalidates the design; they are the cost of choosing a deterministic, honest, local-first system over a more permissive but less trustworthy one.
