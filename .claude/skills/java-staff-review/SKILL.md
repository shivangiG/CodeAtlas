# Java Staff Engineer Review

Perform a critical staff-level review of a Java/Spring Boot change. The review is not advisory — it is a gate. Every item must be explicitly checked and reported.

## When to use

Use this skill after any substantive Java change: new feature, refactor, architecture improvement, or dependency change. Trigger phrases: "review this", "is this good?", "check my changes", "blast radius", or whenever you have made changes in this session and want a pre-commit gate check.

## Review process

### 1. Establish the blast radius

Before checking code quality, map what was changed and what it touches.

- List every file modified, created, or deleted.
- For each changed file, identify its direct callers and dependents (imports, Spring injection, interface implementors).
- Rate blast radius: **contained** (1–2 files, no public API change), **moderate** (3–10 files, interface/DTO change), or **wide** (>10 files, or a seam/contract changed).
- If the blast radius is moderate or wide, state it explicitly at the top of the review.

### 2. Functional correctness — no regression

Check each changed method/class for:

- **Happy path**: does it do what the name and spec say?
- **Edge cases**: empty collections, null inputs, missing optional values, zero/negative numbers, empty strings.
- **Error paths**: are exceptions meaningful, correctly typed, and mapped to the right HTTP status?
- **State integrity**: if the change touches persistent state (JPA, caches), can it leave the system in an inconsistent state?
- **Concurrency**: any shared mutable state accessed without synchronisation?

Flag regressions as **BLOCKER**. Flag uncovered edge cases as **WARNING**.

### 3. SOLID principles

Check each principle. Report violations, not aspirations.

| Principle | What to check |
|-----------|---------------|
| **SRP** — Single Responsibility | Does each class have exactly one reason to change? A class that persists AND validates AND maps is a violation. |
| **OCP** — Open/Closed | Is new behaviour added by extension (new class, new implementation) or by editing existing logic? Switch/if-else chains on type are a signal. |
| **LSP** — Liskov Substitution | Can every implementation of an interface be swapped without callers needing to know? Check for interface methods that only some implementations honour. |
| **ISP** — Interface Segregation | Are interfaces narrow? A service interface with 10+ methods that callers only use 2 of is a violation. |
| **DIP** — Dependency Inversion | Do high-level modules depend on abstractions, not concrete classes? Check `new ConcreteClass()` inside business logic, or `@Autowired ConcreteServiceImpl`. |

### 4. GoF design patterns — applied correctly

Identify which patterns are present. Verify they are applied correctly and not cargo-culted.

**Patterns common in this codebase:**

| Pattern | Where it appears | Correctness check |
|---------|-----------------|-------------------|
| **Repository** (DAO variant) | `SnapshotRepository` seam | Interface returns domain/DTO types, no JPA entity escapes |
| **Adapter** | `SnapshotRepositoryJpaImpl`, controllers | Adapter translates between two incompatible interfaces; no business logic inside |
| **Template Method** | `SnapshotsApi`, `HealthApi` (generated) | Controllers implement the contract; they must not skip required methods |
| **Strategy** | Future: graph indexing strategies | Strategy objects must be substitutable without caller changes |
| **Factory** | MapStruct-generated mappers | Verify factory output type matches what callers expect |
| **Facade** | Service layer over repository + mapper | Facade must simplify, not re-expose complexity |

Flag pattern misuse as **WARNING**. Flag a missing pattern where one is clearly needed as **SUGGESTION**.

### 5. Unit tests

For every changed class:

- Is there a corresponding unit test under `src/test/java/.../unit/`?
- Does the test cover the happy path?
- Does it cover at least one edge case or error path?
- Are mocks used only for collaborators, not for the class under test?
- Are assertions specific (not just `assertThat(result).isNotNull()`)?

Flag missing coverage as **BLOCKER** if the changed method has business logic. Flag weak assertions as **WARNING**.

### 6. Integration tests

For every changed HTTP endpoint or JPA interaction:

- Is there a corresponding integration test under `src/test/java/.../integration/`?
- Does it start a real Spring context (`@SpringBootTest`)?
- Does it hit the endpoint through `MockMvc` end-to-end?
- Does it assert on HTTP status, response body fields, and error cases (4xx)?

Flag missing integration coverage for new/changed endpoints as **BLOCKER**.

---

## Output format

```
## Staff Review — <short description of the change>

### Blast radius: <contained | moderate | wide>
<list of changed files and their dependents>

### Functional correctness
- [BLOCKER | WARNING | OK] <finding>

### SOLID
- [BLOCKER | WARNING | OK] <principle> — <finding>

### GoF patterns
- [BLOCKER | WARNING | OK | SUGGESTION] <pattern> — <finding>

### Unit tests
- [BLOCKER | WARNING | OK] <class> — <finding>

### Integration tests
- [BLOCKER | WARNING | OK] <endpoint> — <finding>

### Verdict
PASS / PASS WITH WARNINGS / BLOCKED — <one-sentence summary>
```

A change is **BLOCKED** if any BLOCKER finding exists. It must not be committed until all BLOCKERs are resolved.
