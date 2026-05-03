# Advanced Java Best Practices

Reference guide for modern Java (17+) best practices used in this codebase.
Apply these standards when writing new code, reviewing existing code, or refactoring.
Each section pairs a rule with a concrete do/don't from this project's domain.

---

## 1. Streams

### Prefer `.toList()` over `Collectors.toList()`

Java 16+ `Stream.toList()` returns an unmodifiable list. Use it everywhere unless the caller explicitly needs a mutable list.

```java
// DO
return jpaRepository.findAll().stream().map(mapper::toDto).toList();

// DON'T
return jpaRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
```

### Terminal operations — match the operation to the intent

| Intent | Use |
|--------|-----|
| Does any element match? | `.anyMatch(predicate)` — not `.filter().findAny().isPresent()` |
| Find the first match | `.filter().findFirst()` — returns `Optional`, never null |
| Transform 1-to-1 | `.map()` |
| Transform and flatten | `.flatMap()` — not nested `.map().stream()` |
| Reduce to a single value | `.reduce()` or specialized: `.count()`, `.min()`, `.max()` |
| Group results | `Collectors.groupingBy()` |
| Partition into two lists | `Collectors.partitioningBy()` |

### Never mutate state inside a stream

Streams must be side-effect-free. Mutations inside `.map()` or `.forEach()` on shared state are a concurrency hazard and a readability trap.

```java
// DO — collect into a new list
List<String> names = snapshots.stream().map(SnapshotResponseDto::snapshotName).toList();

// DON'T — mutating an external list inside a stream
List<String> names = new ArrayList<>();
snapshots.stream().forEach(s -> names.add(s.snapshotName())); // side effect
```

### Avoid `.orElse(null)` — use `.orElseThrow()` or `.orElse(defaultValue)`

```java
// DO
Snapshot active = snapshotRepository.findActive()
        .orElseThrow(() -> new SnapshotNotFoundException("no active snapshot"));

// DON'T — null leaks past the Optional boundary
Snapshot active = snapshotRepository.findActive().orElse(null);
if (active == null) { ... }
```

### Chain `.filter()` before `.map()` — not after

```java
// DO — filter early, map only what you need
snapshots.stream()
         .filter(s -> s.active())
         .map(SnapshotResponseDto::snapshotName)
         .toList();

// DON'T — map everything, then throw most of it away
snapshots.stream()
         .map(SnapshotResponseDto::snapshotName)
         .filter(name -> !name.isEmpty())
         .toList();
```

### Parallel streams — explicit justification required

Never use `.parallelStream()` without a comment stating why parallelism helps here. Wrong for I/O-bound work, small collections, or operations with shared state.

```java
// Only if: CPU-bound, collection > 10k elements, no shared state, measured improvement
return heavyComputations.parallelStream().map(this::compute).toList(); // justified: N=50k, CPU-bound
```

---

## 2. Optional

### Optional is a return type — not a field type or parameter type

```java
// DO — return type only
public Optional<SnapshotResponseDto> findActive() { ... }

// DON'T — Optional as a field
private Optional<String> cachedName; // serialisation breaks, ambiguous null vs absent

// DON'T — Optional as a parameter
public void process(Optional<String> name) { // use overloading or @Nullable instead
```

### Prefer `.map()` and `.flatMap()` over `.isPresent()` + `.get()`

```java
// DO
return snapshotRepository.findActive()
        .map(dto -> new GraphStatusResponseDto(dto.snapshotName(), "fresh", dto.createdAt(), false));

// DON'T
Optional<SnapshotResponseDto> opt = snapshotRepository.findActive();
if (opt.isPresent()) {
    return Optional.of(new GraphStatusResponseDto(opt.get().snapshotName(), ...));
}
return Optional.empty();
```

### `.ifPresent()` for side effects, `.ifPresentOrElse()` for branching

```java
// DO
snapshotRepository.findActive()
        .filter(dto -> dto.snapshotName().equals(snapshotId))
        .ifPresent(dto -> { throw new ActiveSnapshotDeletionException(snapshotId); });

// DON'T
if (snapshotRepository.findActive().isPresent() &&
    snapshotRepository.findActive().get().snapshotName().equals(snapshotId)) { ... } // double call
```

---

## 3. Records

### Use records for all immutable data carriers

Records are the right type for DTOs, domain value objects, configuration holders, and event payloads. They give `equals`, `hashCode`, `toString`, and immutability for free.

```java
// DO
public record SnapshotResponseDto(Long id, String snapshotName, String createdAt) {}

// DON'T — mutable POJO for a DTO
public class SnapshotResponseDto {
    private Long id;
    // ... getters/setters ...
}
```

### Records and validation

Put `@NotBlank`, `@NotNull`, `@Size` on record components for request DTOs. Spring validates them when the record is a `@RequestBody` parameter or when `@Valid` is present.

```java
public record SnapshotRequestDto(
        @NotBlank(message = "snapshotName is required")
        String snapshotName
) {}
```

### Records cannot be JPA entities

JPA requires a mutable no-arg constructor. Use `class` for `@Entity` types — they live in `dto/entity/` and never escape the repository seam.

---

## 4. MapStruct — advanced patterns

### Naming convention

One mapper per entity. Name: `<Entity>Mapper`. Located in `mappers/`. Always `@Mapper(componentModel = "spring")`.

### Type conversions with `expression`

For conversions MapStruct cannot infer (e.g. `Instant` → `String`, enum remapping):

```java
@Mapper(componentModel = "spring")
public interface SnapshotMapper {

    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toString())")
    SnapshotResponseDto toDto(SnapshotJpaEntity entity);
}
```

### Ignore fields not in the target

```java
@Mapping(target = "internalField", ignore = true)
TargetDto toDto(SourceEntity entity);
```

### Collection mapping — free when element mapping is defined

If `SnapshotMapper.toDto(SnapshotJpaEntity)` exists, MapStruct generates `List<SnapshotResponseDto> toDtoList(List<SnapshotJpaEntity>)` for free — just declare it in the interface.

```java
@Mapper(componentModel = "spring")
public interface SnapshotMapper {
    SnapshotResponseDto toDto(SnapshotJpaEntity entity);
    List<SnapshotResponseDto> toDtoList(List<SnapshotJpaEntity> entities); // generated automatically
}
```

Use this instead of `.stream().map(mapper::toDto).toList()` in the adapter — keeps the stream logic inside the generated mapper.

### Nested object mapping

MapStruct maps nested objects automatically if a mapper exists for the nested type. For cross-mapper references use `uses`:

```java
@Mapper(componentModel = "spring", uses = { CommonMapper.class })
public interface SnapshotMapper {
    SnapshotResponseDto toDto(SnapshotJpaEntity entity);
}
```

### Constant and default values

```java
@Mapping(target = "graphStatus", constant = "fresh")          // hardcoded
@Mapping(target = "rebuildWorkerActive", defaultValue = "false") // when source is null
GraphStatusResponseDto toGraphStatus(SnapshotJpaEntity entity);
```

This moves the `new GraphStatusResponseDto(dto.snapshotName(), "fresh", dto.createdAt(), false)` construction from `SnapshotServiceImpl` into the mapper where it belongs.

### Verify generated code

After any mapper change, run `./gradlew compileJava` and inspect `build/generated/sources/annotationProcessor/.../mappers/<Entity>MapperImpl.java` to confirm the generated implementation matches intent.

---

## 5. Immutability and defensive copies

### Prefer `List.of()` / `Map.of()` for literals

```java
// DO
private static final List<String> VALID_STATUSES = List.of("fresh", "stale_for_relevant_files", "no_graph");

// DON'T
private static final List<String> VALID_STATUSES = new ArrayList<>(Arrays.asList("fresh", ...));
```

### Return unmodifiable collections from service/repository methods

```java
// DO — Stream.toList() is already unmodifiable
return jpaRepository.findAll().stream().map(mapper::toDto).toList();

// DON'T — callers can mutate the returned list
return new ArrayList<>(mappedResults);
```

### Never expose internal mutable state

If a class holds a `List` field, return a defensive copy or unmodifiable view:

```java
// DO
public List<String> getTags() {
    return Collections.unmodifiableList(this.tags);
}
```

---

## 6. Null discipline

### Never return null from a method — use Optional or throw

```java
// DO
public Optional<SnapshotResponseDto> findByName(String name) { ... }

// DON'T
public SnapshotResponseDto findByName(String name) {
    // ...
    return null; // forces callers to null-check
}
```

### Constructor injection eliminates null injection

Spring's constructor injection guarantees non-null collaborators at startup. Never use field injection (`@Autowired` on a field) — it allows null injection and hides dependencies.

```java
// DO
public SnapshotServiceImpl(SnapshotRepository snapshotRepository, TimeProvider timeProvider) {
    this.snapshotRepository = snapshotRepository;
    this.timeProvider = timeProvider;
}

// DON'T
@Autowired
private SnapshotRepository snapshotRepository; // hidden dependency, mockable only via reflection
```

---

## 7. Modern Java 17 features

### Switch expressions over switch statements

```java
// DO — exhaustive, returns a value, no fall-through
String label = switch (confidence) {
    case HIGH   -> "authoritative";
    case MEDIUM -> "inferred";
    case LOW    -> "weak";
};

// DON'T
String label;
switch (confidence) {
    case HIGH: label = "authoritative"; break;
    // ... easy to forget break
}
```

### Text blocks for multiline strings (SQL, JSON, YAML in tests)

```java
// DO
String json = """
        {
          "snapshotName": "S-01"
        }
        """;

// DON'T
String json = "{\"snapshotName\": \"S-01\"}";
```

### Pattern matching for `instanceof`

```java
// DO
if (exception instanceof SnapshotNotFoundException notFound) {
    log.warn("Not found: {}", notFound.getMessage());
}

// DON'T
if (exception instanceof SnapshotNotFoundException) {
    SnapshotNotFoundException notFound = (SnapshotNotFoundException) exception;
    log.warn("Not found: {}", notFound.getMessage());
}
```

---

## 8. Exception discipline

### Domain exceptions are unchecked (`RuntimeException`)

All exceptions in `exception/` extend `RuntimeException`. Never use checked exceptions for domain error cases — they leak implementation details through interface signatures.

### Be specific, not generic

```java
// DO
throw new SnapshotNotFoundException(snapshotId);

// DON'T
throw new RuntimeException("not found"); // loses type information for the handler
throw new Exception("not found");        // forces callers to catch or declare
```

### Log at the boundary — not inside domain code

Exception handlers (`GlobalExceptionHandler`) are the right place to log errors. Domain services and repositories should throw; they should not catch-and-log.

---

## 9. Spring-specific

### `@Transactional` placement

- Put `@Transactional` on the **implementation** method (`SnapshotRepositoryJpaImpl`), not on the interface.
- Write operations only: `save`, `delete`, bulk updates. Read operations do not need it unless you need a read-only transaction hint.
- Never put `@Transactional` on controllers — transactions must not span HTTP boundaries.

### `TimeProvider` — never call `Instant.now()` directly

All `Instant.now()` calls go through `TimeProvider`. This makes time deterministic in unit tests.

```java
// DO
Instant now = timeProvider.now();

// DON'T
Instant now = Instant.now(); // impossible to control in tests
```

### `@ConfigurationProperties` over `@Value`

Bind configuration as typed records/classes using `@ConfigurationProperties`. `@Value` is fragile (string-based, no IDE support, no validation).

```java
// DO
@ConfigurationProperties(prefix = "atlas")
public class AtlasProperties {
    private String appName = "codeatlas";
    // ...
}

// DON'T
@Value("${atlas.app-name}")
private String appName;
```

---

## Quick anti-pattern index

| Anti-pattern | Rule |
|---|---|
| `stream().collect(Collectors.toList())` | Use `.toList()` |
| `Optional.get()` without `isPresent()` check | Use `.orElseThrow()` or `.map()` |
| `Optional` as a field or parameter | Return type only |
| Mutable DTO class | Use `record` |
| Hand-written field mapping in service/adapter | Use MapStruct `@Mapper` |
| `Instant.now()` in business code | Use `TimeProvider` |
| `@Autowired` field injection | Constructor injection |
| `return null` from a method | Return `Optional` or throw |
| `@Transactional` on a controller | Transactions end at the service layer |
| `new ArrayList<>()` for a known literal list | `List.of()` |
| `switch` statement with `break` | Switch expression |
| Catch-and-log in domain code | Throw; log at the handler |
