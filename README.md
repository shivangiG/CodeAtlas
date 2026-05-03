# CodeAtlas

A self-hosted repository intelligence service. Indexes a codebase into a navigable graph and exposes it over a REST API so AI coding agents (and humans) can answer questions like "what is the blast radius of changing this file?", "where does this capability already exist?", and "what is the minimal context I need to read to implement this task?".

---

## Modules

```
codeatlas/
├── openapi-spec/        OpenAPI 3.1.0 contract + code generation
│                        Produces: Spring interfaces, model classes
├── server/              Spring Boot application
│                        Implements the generated interfaces
├── service-client/      Typed OkHttp client for the CodeAtlas API
│                        Used by integration tests and downstream consumers
└── integration-tests/   Black-box HTTP tests via the service client
                         Starts server at a random port, no MockMvc
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 23 |
| Gradle (wrapper) | 8.x (use `./gradlew`) |

---

## Running locally

```bash
./gradlew :server:bootRun --args='--spring.profiles.active=local'
```

The `local` profile uses a file-based H2 database at `~/codeatlas-db` (see `server/src/main/resources/application-local.yml`).

---

## Build and test

```bash
# Full build including all unit and integration tests
./gradlew clean build

# Server only (faster feedback loop)
./gradlew :server:build

# Integration tests only
./gradlew :integration-tests:test
```

---

## API domains

| Prefix | Status | Description |
|---|---|---|
| `GET /health` | Implemented | Liveness check |
| `POST /snapshots` | Implemented | Create a named index snapshot |
| `GET /snapshots` | Implemented | List all snapshots |
| `GET /snapshots/active` | Implemented | Get the currently active snapshot |
| `DELETE /snapshots/{id}` | Implemented | Delete a snapshot (not allowed if active) |
| `GET /graph/summary` | 501 | High-level module/package map |
| `GET /graph/symbols` | 501 | Search symbols by name pattern |
| `GET /graph/symbols/{id}` | 501 | Single symbol with edges |
| `GET /graph/call-flows` | 501 | Trace call path from a symbol |
| `POST /graph/task-contexts` | 501 | Minimal file set for a task |
| `POST /impacts` | 501 | Blast radius for a changed file/symbol |
| `POST /capabilities/search` | 501 | Find existing capability to reuse |
| `GET /capabilities/clients` | 501 | List internal service clients |
| `GET /analyzers/violations` | 501 | Architecture violation findings |
| `GET /analyzers/openapi-drift` | 501 | Spec-vs-code drift findings |
| `GET /analyzers/reviews` | 501 | Full architecture review report |
| `POST /ci/gates` | 501 | Evaluate CI gate (BLOCK/WARN/PASS) |
| `POST /ci/baselines` | 501 | Capture current findings as baseline |

The full OpenAPI 3.1.0 spec lives in `openapi-spec/src/main/openapi/codeatlas.yaml`.

---

## Architecture

```
HTTP request
    └── Controller (implements generated *Api interface)
            └── Service (business rules, accepts/returns DTOs)
                    └── Repository interface (domain boundary)
                            └── RepositoryJpaImpl (JPA adapter)
                                    └── JpaRepository (Spring Data)
```

Key invariants:
- **Controllers are adapters only** — no business logic.
- **JPA entities never escape `repositories/`** — services and controllers see DTOs.
- **Exception → HTTP status mapping** lives exclusively in `GlobalExceptionHandler`.
- **Every endpoint** has a unit test in `server/` and a real HTTP test in `integration-tests/`.
- **Unimplemented endpoints** return `HTTP 501`. Search `NOT_IMPLEMENTED` to enumerate gaps.

---

## Agent guidance

See [`CLAUDE.md`](CLAUDE.md) for the full set of coding standards, SOLID rules, GoF patterns in use, and available skills.

Key skills in `.claude/skills/`:

| Skill | When to use |
|---|---|
| `openapi-first` | Adding or changing any HTTP endpoint |
| `new-domain-entity` | Adding a new JPA-backed domain concept |
| `java-staff-review` | Before committing any substantive Java change |
| `prepare-pr` | Before opening a pull request |
| `dead-code-removal` | After any significant refactor |
| `clean-code` | Naming, size, cohesion, comment discipline |
| `java-best-practices` | Java 17+ patterns (records, Optional, MapStruct) |
