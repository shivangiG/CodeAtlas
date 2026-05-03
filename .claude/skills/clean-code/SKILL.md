# Clean Code

Enforce clean code standards for every Java file touched in this codebase.
These rules apply during authoring, code review, and refactoring.
They are not stylistic preferences — violations are flagged as defects.

---

## 1. Naming

### Names must reveal intent — no abbreviations, no type suffixes in variables

```java
// DO
List<SnapshotResponseDto> snapshots = snapshotService.listSnapshots();
Optional<SnapshotResponseDto> activeSnapshot = snapshotRepository.findActive();

// DON'T
List<SnapshotResponseDto> snpList = ...;   // abbreviation
Optional<SnapshotResponseDto> opt = ...;   // generic non-name
SnapshotResponseDto snapshotDto = ...;     // type suffix in variable name — the type already says DTO
```

### Method names are verbs. Class names are nouns. No "Manager", "Helper", "Util" suffixes

```java
// DO   — verbs for methods
createSnapshot(), findActive(), deleteByName(), evaluateCiGate()

// DO   — nouns for classes
SnapshotService, GlobalExceptionHandler, AtlasProperties, TimeProvider

// DON'T
SnapshotManager    // too generic — what does it "manage"?
SnapshotHelper     // what does it "help" with?
SnapshotUtils      // put static helpers in the class that owns them, or make them proper services
```

### Boolean names are predicates

```java
// DO
boolean isActive, boolean hasActiveSnapshot, boolean rebuildWorkerActive

// DON'T
boolean active       // is it a flag or a state? ambiguous in isolation
boolean status       // not a predicate
```

### Constants are UPPER_SNAKE_CASE in `constants/`

```java
// DO
public static final String API_BASE = "/api/v1";

// DON'T
public static final String apiBase = "/api/v1";
public static final String Api_Base = "/api/v1";
```

---

## 2. Methods

### One level of abstraction per method

A method either coordinates calls to other methods (high level) or does low-level work — never both.

```java
// DO — high level: coordinates
public SnapshotResponseDto createSnapshot(SnapshotRequestDto request) {
    assertNoDuplicate(request.snapshotName());
    return snapshotRepository.save(request.snapshotName(), timeProvider.now());
}
private void assertNoDuplicate(String name) {
    if (snapshotRepository.findByName(name).isPresent()) {
        throw new DuplicateSnapshotException(name);
    }
}

// DON'T — mixes coordination with low-level string check
public SnapshotResponseDto createSnapshot(SnapshotRequestDto request) {
    Optional<SnapshotResponseDto> existing = snapshotRepository.findByName(request.snapshotName());
    if (existing.isPresent()) {
        throw new DuplicateSnapshotException(request.snapshotName());
    }
    return snapshotRepository.save(request.snapshotName(), timeProvider.now());
}
```

### Methods do one thing — name it, then do only that

If a method name requires "and" to describe it, split it.

```java
// DON'T — two responsibilities
public SnapshotResponseDto validateAndSave(SnapshotRequestDto request) { ... }

// DO
public void validate(SnapshotRequestDto request) { ... }
public SnapshotResponseDto save(SnapshotRequestDto request) { ... }
```

### Max 3 parameters — use a record for more

```java
// DO — request object groups related params
public SnapshotResponseDto createSnapshot(SnapshotRequestDto request) { ... }

// DON'T
public SnapshotResponseDto createSnapshot(String name, Instant time, boolean active, String source) { ... }
```

### Avoid flag arguments — split into two methods

```java
// DON'T
public List<SnapshotResponseDto> findSnapshots(boolean activeOnly) { ... }

// DO
public List<SnapshotResponseDto> findAll() { ... }
public Optional<SnapshotResponseDto> findActive() { ... }
```

---

## 3. Classes

### Small and focused — the Single Responsibility test

Ask: "What is this class responsible for?" The answer must fit in one sentence without "and".

- `SnapshotRepositoryJpaImpl` — adapts JPA to the `SnapshotRepository` interface.
- `SnapshotServiceImpl` — enforces snapshot business rules (duplicate, active-deletion).
- `GlobalExceptionHandler` — maps domain exceptions to HTTP error responses.

If you cannot describe a class in one sentence, it has too many responsibilities.

### No static utility classes — give behaviour to the objects that own it

```java
// DON'T
public class SnapshotUtils {
    public static String formatCreatedAt(Instant instant) { ... }
}

// DO — put the conversion in the MapStruct mapper where it belongs
@Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toString())")
```

### Constructors only assign — no logic

```java
// DO
public SnapshotServiceImpl(SnapshotRepository snapshotRepository, TimeProvider timeProvider) {
    this.snapshotRepository = snapshotRepository;
    this.timeProvider = timeProvider;
}

// DON'T
public SnapshotServiceImpl(SnapshotRepository snapshotRepository, TimeProvider timeProvider) {
    this.snapshotRepository = Objects.requireNonNull(snapshotRepository); // logic in constructor
    this.timeProvider = timeProvider;
    init(); // side effect in constructor
}
```

---

## 4. Comments

### Comments explain *why* — not *what*

The code explains what. A comment that restates what the code does is noise.

```java
// DON'T — restates the code
// Check if snapshot name exists
if (snapshotRepository.findByName(request.snapshotName()).isPresent()) {

// DO — explains why
// Duplicate check before save to produce a 409 rather than letting the DB
// UniqueConstraint throw a DataIntegrityViolationException with no HTTP mapping.
if (snapshotRepository.findByName(request.snapshotName()).isPresent()) {
```

### No commented-out code — delete it, git remembers

```java
// DON'T
// SnapshotMapper mapper = new SnapshotMapper(); // old approach
// private final SnapshotMapper snapshotMapper; // removed in refactor
```

### No TODO comments without a GitHub issue reference

```java
// DON'T
// TODO: implement this

// DO
// TODO: implement — tracked in github.com/shivangiG/CodeAtlas/issues/42
// or: remove this stub when the graph indexer is implemented (issue #42)
```

---

## 5. Error handling

### Never swallow exceptions

```java
// DON'T
try {
    snapshotRepository.deleteByName(name);
} catch (Exception e) {
    // silent
}

// DO — let it propagate, or translate to a domain exception
snapshotRepository.deleteByName(name); // throws if not found — caller handles it
```

### Never use exceptions for flow control

```java
// DON'T
try {
    return snapshotRepository.findByName(name).get(); // throws NoSuchElementException
} catch (NoSuchElementException e) {
    return defaultSnapshot;
}

// DO
return snapshotRepository.findByName(name).orElse(defaultSnapshot);
```

---

## 6. Tests as clean code

### Test names describe behaviour — not implementation

```java
// DO
void createSnapshot_throwsDuplicate_whenNameExists()
void deleteSnapshot_returns409_whenDeletingActive()
void getActiveSnapshot_returnsFreshStatus()

// DON'T
void test1()
void testCreateSnapshot()
void snapshotCreation()
```

### Arrange / Act / Assert — one blank line between sections

```java
@Test
void createSnapshot_delegatesToRepository() {
    // Arrange
    when(snapshotRepository.findByName("S1")).thenReturn(Optional.empty());
    when(timeProvider.now()).thenReturn(NOW);
    when(snapshotRepository.save(eq("S1"), any())).thenReturn(S1);

    // Act
    var result = service.createSnapshot(new SnapshotRequestDto("S1"));

    // Assert
    assertThat(result.snapshotName()).isEqualTo("S1");
}
```

### One logical assertion per test — not one `assertThat` call

Multiple `assertThat` calls are fine when they verify the same logical outcome. A test that verifies both creation success *and* an unrelated field on a different object is two tests.

---

## Quick smell index

| Smell | Fix |
|---|---|
| Method name needs "and" | Split into two methods |
| Method has > 3 params | Introduce a request record |
| Boolean parameter | Split into two named methods |
| Comment restates code | Delete the comment |
| Commented-out code | Delete it |
| `catch (Exception e) {}` | Propagate or translate |
| `Utils` / `Helper` / `Manager` class | Assign behaviour to the owning class |
| Variable named `opt`, `dto`, `obj` | Name it after what it represents |
| TODO with no issue reference | Add issue number or delete |
| Test named `test1` or `testFoo` | Rename to `foo_doesX_whenY` |
