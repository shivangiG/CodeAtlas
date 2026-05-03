# Dead Code Removal

Identify and safely delete dead code — code that is unreachable, unused, or has no effect.
Dead code is a liability: it misleads readers, inflates diffs, and hides real logic.

## When to use

Trigger this skill when:
- Preparing a PR (called by `prepare-pr` skill)
- A refactor leaves orphaned classes or methods
- You hear "clean up dead code", "remove unused code", "find dead code"
- After a large architectural change (e.g. removing a layer, changing a return type)

---

## Categories of dead code

### 1. Unused imports

Every unused import is dead code. Fix before committing.

```java
// DON'T — unused imports are dead code
import com.sgarsgaya.codeatlas.domain.Snapshot;        // deleted when domain/ was removed
import com.sgarsgaya.codeatlas.mappers.SnapshotMapper;  // deleted when mapper was absorbed
import java.util.ArrayList;                             // replaced by Stream.toList()
```

**How to find:** `./gradlew compileJava` with `-Xlint:all` flags warns on unused imports. Also: IDE "Optimize Imports" or `rg "^import" --include="*.java" | sort | uniq` to spot duplicates.

### 2. Unused fields

A private field that is assigned but never read, or declared but never assigned.

```java
// DON'T
public class SnapshotServiceImpl {
    private final SnapshotMapper snapshotMapper; // injected but removed when MapStruct took over
```

**How to find:** `rg "private final" src/main/java --include="*.java"` and verify each field is referenced.

### 3. Unused methods

Private methods with zero call sites. Public methods on internal classes (`@Repository`, `@Service`) with zero callers inside the module.

```java
// DON'T — private method, zero call sites
private SnapshotResponseDto mapToResponse(SnapshotJpaEntity entity) { ... }
```

**How to find:**
```bash
# Find private methods — verify each has at least one call site
rg "private .+ \w+\(" src/main/java --include="*.java" -n
```

### 4. Unreachable code

Code after a `return`, `throw`, or exhaustive `switch` in the same block.

```java
// DON'T
public ResponseEntity<Void> deleteSnapshot(String id) {
    snapshotService.deleteSnapshot(id);
    return ResponseEntity.noContent().build();
    log.info("Deleted"); // unreachable — after return
}
```

### 5. Orphaned classes

Classes with no remaining callers after a refactor. Common sources:
- Domain types removed from a seam (e.g. `domain/Snapshot.java` after MapStruct was introduced)
- Old mapper classes after MapStruct replaced them (`mappers/SnapshotMapper.java` hand-written version)
- Stub classes kept after the real implementation replaced them

**How to find:**
```bash
# Find all top-level class names in main source
rg "^public class|^public record|^public interface" src/main/java --include="*.java" -l

# Then verify each is imported somewhere
rg "<ClassName>" src/main/java src/test/java --include="*.java"
```

### 6. Dead configuration properties

`AtlasProperties` fields or `application.yml` keys that nothing reads.

**How to find:**
```bash
# Find all @ConfigurationProperties fields
rg "private .+ \w+;" src/main/java/com/sgarsgaya/codeatlas/config --include="*.java"

# Verify each field name is referenced in source
rg "getAppName\|appName" src/main/java --include="*.java"
```

### 7. Commented-out code

See `clean-code` skill. Commented-out code is always dead code. Delete it — git remembers.

### 8. Unused dependencies in `build.gradle`

Dependencies that are declared but nothing imports from them.

**Current candidates to verify:**
- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` — verify it is used for YAML parsing
- `org.yaml:snakeyaml` — verify it is not just a transitive override
- `com.github.javaparser:javaparser-symbol-solver-core` — graph indexer not yet implemented; this is forward-looking. Keep but annotate.

```gradle
// Mark forward-looking dependencies clearly
implementation 'com.github.javaparser:javaparser-symbol-solver-core:3.26.2' // used by: graph indexer (not yet implemented — issue #N)
```

---

## Removal process

### Before deleting anything

1. **Confirm zero call sites** — do not rely on IDE alone. Run:
   ```bash
   rg "<SymbolName>" src/ --include="*.java"
   ```
2. **Check test code too** — a class may be unused in main but referenced in tests (that's still used).
3. **Check generated code** — generated interfaces (`*Api`) may reference model classes; confirm the generator doesn't need the class.

### Delete in this order

1. Remove the dead code (class, method, field, import).
2. Recompile: `./gradlew compileJava` — must succeed.
3. Run tests: `./gradlew test` — must succeed.
4. Repeat until clean.

### Never delete without compiling

Deleting a class that has undiscovered callers will break the build silently if you don't verify. Always compile after each deletion.

---

## Scope for this project — known dead code candidates

Run these checks after every significant refactor:

| Symbol | Likely dead after | Verify with |
|--------|-------------------|-------------|
| `domain/Snapshot.java` | MapStruct refactor | `rg "Snapshot" src --include="*.java"` — should only match `SnapshotResponseDto`, `SnapshotService`, etc. |
| `mappers/SnapshotMapper.java` (old hand-written) | MapStruct refactor | Already deleted — confirm no ghost import |
| `findBySnapshotName` on `SnapshotJpaRepository` | If replaced by `findByActiveTrue` | `rg "findBySnapshotName"` |
| Any `import com.sgarsgaya.codeatlas.domain` | domain package removed | `rg "codeatlas\.domain"` — must return zero results |
| Stub controller `TODO` comments | When real impl replaces stub | `rg "NOT_IMPLEMENTED"` to enumerate remaining stubs |

---

## Output format

After running this skill, report:

```
## Dead Code Report

### Removed
- [file] [symbol] — reason

### Kept (with justification)
- [file] [symbol] — reason (e.g. "forward-looking, tracked in issue #N")

### Could not verify
- [file] [symbol] — needs human check (e.g. reflection, dynamic loading)
```
