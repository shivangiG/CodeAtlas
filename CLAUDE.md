# CodeAtlas

## Agent skills

### Issue tracker

Issues live in GitHub Issues on `github.com/shivangiG/CodeAtlas`. See `.claude/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `.claude/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `.claude/agents/domain.md`.

### Java staff review

Before committing any substantive Java change, run `.claude/skills/java-staff-review/SKILL.md`. Gates on blast radius, functional correctness, SOLID, GoF patterns, unit tests, and integration tests. A BLOCKER finding blocks the commit.

### OpenAPI-first

When adding or changing any HTTP endpoint, follow `.claude/skills/openapi-first/SKILL.md`. Spec entry → generate → 501 stub (compiles) → implement → tests. No implementation before a spec entry exists.

### New domain entity

When adding a new JPA-backed domain concept, follow `.claude/skills/new-domain-entity/SKILL.md`.

### Clean code

Naming, method size, class cohesion, comment discipline, and error handling rules. See `.claude/skills/clean-code/SKILL.md`. Apply during authoring and review — violations are defects, not preferences.

### Prepare PR

Pre-PR gate: staff review → build + test → dead code check → self-review diff → PR description → open PR → CI green. See `.claude/skills/prepare-pr/SKILL.md`. Do not open a PR until every step passes.

### Dead code removal

Find and safely delete unused imports, fields, methods, orphaned classes, dead config, and commented-out code. See `.claude/skills/dead-code-removal/SKILL.md`. Run after every significant refactor and as part of PR preparation.

### Java best practices

Advanced Java 17+ standards for this codebase. See `.claude/skills/java-best-practices/SKILL.md`. Covers: streams (`.toList()`, no side-effects, filter-before-map), Optional (return type only, prefer `.map()`/`.orElseThrow()`), records for all DTOs, MapStruct advanced patterns (collection mapping, constants, `uses`), immutability (`List.of()`), null discipline (no `return null`, constructor injection), modern Java 17 features (switch expressions, text blocks, pattern matching), exception discipline, and Spring conventions (`TimeProvider`, `@ConfigurationProperties`, `@Transactional` placement). Covers all 10 steps: entity → JPA repo → DTOs → MapStruct mapper → domain repo interface → adapter → exceptions → service → controller → tests.

---

## Code standards

### Architecture

- Package layout: `controllers/`, `service/`, `service/impl/`, `repositories/`, `dto/`, `dto/entity/`, `mappers/`, `config/`, `constants/`, `util/`, `exception/`
- Controllers implement generated OpenAPI interfaces from `:openapi-spec`. They are adapters — no business logic.
- Services own all business rules. They accept and return DTOs. No JPA types cross the service interface.
- `SnapshotRepository` (and future domain repositories) return DTOs. JPA entities never escape the `repositories/` package.
- MapStruct `@Mapper(componentModel = "spring")` interfaces live in `mappers/`. No hand-written field-by-field mapping elsewhere.
- Exceptions are domain types in `exception/`. HTTP status mapping lives in `GlobalExceptionHandler`, not in controllers or services.

### SOLID

- **SRP**: one class, one reason to change. Controllers map HTTP ↔ service. Services orchestrate. Repositories persist. Mappers translate.
- **OCP**: new behaviour via new classes/implementations, not by editing existing switch/if-else chains.
- **DIP**: inject interfaces, never concrete implementations. No `new ConcreteServiceImpl()` in business code.

### GoF patterns in use

| Pattern | Where |
|---------|-------|
| Repository (DAO) | `SnapshotRepository` — entity never escapes |
| Adapter | `SnapshotRepositoryJpaImpl`, all controllers |
| Factory | MapStruct-generated mapper implementations |
| Facade | Service layer over repository + mapper |
| Template Method | Generated `*Api` interfaces — controllers implement the contract |

### Testing

- Unit tests: `server/src/test/java/.../unit/` — Mockito + AssertJ, no Spring context, mock collaborators only.
- Integration tests: `integration-tests/` module — extend `IntegrationTestBase`, make real HTTP calls via `CodeAtlasClient` (OkHttp). App starts at `RANDOM_PORT`. No MockMvc.
- `server/` keeps `ApplicationContextIntegrationTest` as a Spring context smoke test only.
- Every new endpoint needs: a unit test for the service in `server/`, and a real HTTP test in `integration-tests/` via `CodeAtlasClient`.
- Every new exception path must assert the correct HTTP status and `ErrorResponse.code` field in the integration test.

### Stub controllers

Unimplemented endpoints return `HTTP 501`. Search `NOT_IMPLEMENTED` to enumerate all gaps. Replace the stub with a real implementation when the backing subsystem is ready — do not silently delete the stub.
