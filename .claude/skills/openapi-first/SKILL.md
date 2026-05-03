# OpenAPI-First Development

Enforce the contract-first workflow for every new or changed HTTP endpoint in CodeAtlas.
No Java implementation may exist for an endpoint that has no spec entry.
No spec entry is complete until a controller exists — even as a 501 stub.

## When to use

Trigger this skill when:
- Adding a new endpoint to the API
- Changing an existing endpoint's request/response shape
- Implementing a stub controller (converting 501 → real)
- You hear "add an endpoint", "new route", "new API", "change the request/response"

## The five steps — in order, no skipping

### Step 1 — Write the spec entry

All spec changes happen in `openapi-spec/src/main/openapi/`.

1. Choose the right feature directory (`snapshots/`, `graph/`, `impact/`, etc.). If none fits, create a new one with `path.yaml` and `schema.yaml`.
2. Add the path to `<feature>/path.yaml`. Include:
   - `operationId` (camelCase, unique across all features — this becomes the Java method name)
   - `tags: [<feature>]`
   - `summary` (one line, imperative: "Create a snapshot", not "Creates a snapshot")
   - All `requestBody` and `responses` entries including 4xx error cases
   - `$ref` to schemas in `<feature>/schema.yaml` or `common/schema.yaml` — no inline schemas
3. Add or extend schemas in `<feature>/schema.yaml`. Use `$ref: '../common/schema.yaml#/ErrorResponse'` for error bodies.
4. Register the new path in `openapi-spec/src/main/openapi/codeatlas.yaml` under `paths:` and any new schemas under `components/schemas:`.
5. Verify the spec is valid: `./gradlew :openapi-spec:openApiGenerate` must succeed before proceeding.

**Gate**: if step 5 fails, fix the spec. Do not proceed to step 2.

### Step 2 — Regenerate and verify the generated interface

```bash
./gradlew :openapi-spec:openApiGenerate
```

Confirm that:
- The new `operationId` appears as a method in the generated `*Api` interface under `openapi-spec/build/generated/src/main/java/com/sgarsgaya/codeatlas/api/`.
- All request/response model classes were generated in `com/sgarsgaya/codeatlas/model/`.
- No compilation errors in `:openapi-spec`.

### Step 3 — Create or update the controller stub

Find or create the controller in `src/main/java/com/sgarsgaya/codeatlas/controllers/`.

**New feature** (no controller yet):

```java
// TODO: implement — returns 501 until <subsystem> is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class <Feature>Controller implements <Feature>Api {

    @Override
    public ResponseEntity<<ResponseType>> <operationId>(<params>) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
```

**Existing controller** (adding a method):
- Add the `@Override` method returning `HttpStatus.NOT_IMPLEMENTED`.
- Confirm the project compiles: `./gradlew compileJava` must succeed.

**Gate**: the project must compile with the stub before implementing any logic.

### Step 4 — Implement the endpoint

Only after steps 1–3 pass:

1. Add the service method to the `<Domain>Service` interface.
2. Implement it in `service/impl/<Domain>ServiceImpl`. Business logic lives here — not in the controller.
3. If persistence is needed: add to `<Domain>Repository` interface, implement in `<Domain>RepositoryJpaImpl` using the established MapStruct mapper pattern.
4. Add/extend exception types in `exception/` if new error cases are introduced. Map them in `GlobalExceptionHandler`.
5. Replace the `NOT_IMPLEMENTED` stub in the controller with the real delegation call. Controllers contain no business logic — one line per method: call service, map result, return `ResponseEntity`.

### Step 5 — Write the tests

**Unit test** (`src/test/java/.../unit/`):
- One test class per changed service class.
- Cover: happy path, duplicate/conflict case, not-found case, any new edge case introduced.
- Mock all collaborators (`SnapshotRepository`, `TimeProvider`). No Spring context.

**Integration test** (`src/test/java/.../integration/`):
- Extend the relevant `*ControllerIntegrationTest` (or create one).
- Use `@SpringBootTest` + `MockMvc`.
- Assert: correct HTTP status, response body fields, error response `code` field for each 4xx.
- Use `@DirtiesContext` if the test writes to the H2 database.

**Gate**: `./gradlew test` must pass before the change is considered done.

---

## Quick reference — file locations

| What | Where |
|------|-------|
| New path definition | `openapi-spec/src/main/openapi/<feature>/path.yaml` |
| New schema definition | `openapi-spec/src/main/openapi/<feature>/schema.yaml` |
| Root assembly manifest | `openapi-spec/src/main/openapi/codeatlas.yaml` |
| Generated interfaces | `openapi-spec/build/generated/.../api/` (do not edit) |
| Controller | `server/src/main/java/.../controllers/<Feature>Controller.java` |
| Service interface | `server/src/main/java/.../service/<Domain>Service.java` |
| Service impl | `server/src/main/java/.../service/impl/<Domain>ServiceImpl.java` |
| Repository interface | `server/src/main/java/.../repositories/<Domain>Repository.java` |
| JPA adapter | `server/src/main/java/.../repositories/<Domain>RepositoryJpaImpl.java` |
| MapStruct mapper | `server/src/main/java/.../mappers/<Domain>Mapper.java` |
| Exception types | `server/src/main/java/.../exception/<Domain>*.java` |
| HTTP status mapping | `server/src/main/java/.../controllers/GlobalExceptionHandler.java` |
| Unit tests | `server/src/test/java/.../unit/` |
| Integration tests | `integration-tests/src/test/java/.../integration/` |

## Anti-patterns — never do these

- Do not write a controller method before the spec entry exists.
- Do not put business logic in a controller — one delegation call only.
- Do not inline schemas in `codeatlas.yaml` — always `$ref` to feature or common schema files.
- Do not return a JPA entity type from `<Domain>Repository` — return DTOs via MapStruct.
- Do not skip the 501 stub step — the project must compile at every step.
