# New Domain Entity

Add a new JPA-backed domain entity to CodeAtlas following the established layered pattern.
Every layer is created in order. No layer is skipped. The pattern mirrors the `Snapshot` entity exactly.

## When to use

Trigger this skill when:
- Adding a new persistent domain concept (e.g. `GraphIndex`, `CapabilityEntry`, `CiBaseline`)
- You hear "add a new entity", "persist X", "new domain object", "store Y in the database"

Replace `<Entity>` throughout with the PascalCase name of the new entity (e.g. `GraphIndex`).
Replace `<entity>` with the camelCase name (e.g. `graphIndex`).
Replace `<entities>` with the camelCase plural (e.g. `graphIndexes`).

---

## Step 1 — JPA entity in `dto/entity/`

Create `server/src/main/java/com/sgarsgaya/codeatlas/dto/entity/<Entity>JpaEntity.java`:

```java
package com.sgarsgaya.codeatlas.dto.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "<entities>")
public class <Entity>JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Add domain-specific @Column fields here.
    // All columns non-null by default. Add unique = true where a natural key exists.

    protected <Entity>JpaEntity() {}

    public <Entity>JpaEntity(/* domain fields */) {
        // assign fields
    }

    // Getters only — no setters except for mutable state fields.
}
```

Rules:
- Entity lives in `dto/entity/` — **never** in `repositories/` or `domain/`.
- No business logic. No service calls. No mappers.
- Protected no-arg constructor required by JPA.
- Expose only getters from the public API of the entity.

---

## Step 2 — Spring Data JPA repository interface

Create `src/main/java/com/sgarsgaya/codeatlas/repositories/<Entity>JpaRepository.java`:

```java
package com.sgarsgaya.codeatlas.repositories;

import com.sgarsgaya.codeatlas.dto.entity.<Entity>JpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface <Entity>JpaRepository extends JpaRepository<<Entity>JpaEntity, Long> {

    // Add derived query methods for lookups needed by the domain.
    // Example: Optional<<Entity>JpaEntity> findByNaturalKey(String key);
}
```

Rules:
- Derived query method names must match field names in `<Entity>JpaEntity`.
- Use `@Modifying` + `@Query` for bulk updates (e.g. deactivate-all pattern).
- This interface is **never injected outside `repositories/`** — only `<Entity>RepositoryJpaImpl` uses it.

---

## Step 3 — Response DTO

Create `src/main/java/com/sgarsgaya/codeatlas/dto/<Entity>ResponseDto.java`:

```java
package com.sgarsgaya.codeatlas.dto;

public record <Entity>ResponseDto(Long id, /* domain fields as primitives/Strings */) {}
```

Create `src/main/java/com/sgarsgaya/codeatlas/dto/<Entity>RequestDto.java` if the entity is created via an API:

```java
package com.sgarsgaya.codeatlas.dto;

import jakarta.validation.constraints.NotBlank;

public record <Entity>RequestDto(
        @NotBlank(message = "<field> is required")
        String <field>
) {}
```

Rules:
- Records only — no mutable POJOs for DTOs.
- `Instant` → `String` for all timestamp fields (MapStruct handles conversion).
- No JPA annotations. No persistence concerns.

---

## Step 4 — MapStruct mapper

Create `src/main/java/com/sgarsgaya/codeatlas/mappers/<Entity>Mapper.java`:

```java
package com.sgarsgaya.codeatlas.mappers;

import com.sgarsgaya.codeatlas.dto.<Entity>ResponseDto;
import com.sgarsgaya.codeatlas.dto.entity.<Entity>JpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface <Entity>Mapper {

    // Add @Mapping for any field that needs expression-based or named conversion.
    // Example for Instant → String:
    // @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toString())")
    <Entity>ResponseDto toDto(<Entity>JpaEntity entity);
}
```

Rules:
- `componentModel = "spring"` — MapStruct generates a Spring bean, injected by constructor.
- Fields with matching names are mapped automatically.
- Use `expression = "java(...)"` for type conversions (`Instant` → `String`, enum conversions).
- No hand-written field-by-field mapping anywhere else in the codebase.

---

## Step 5 — Domain repository interface

Create `src/main/java/com/sgarsgaya/codeatlas/repositories/<Entity>Repository.java`:

```java
package com.sgarsgaya.codeatlas.repositories;

import com.sgarsgaya.codeatlas.dto.<Entity>ResponseDto;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence seam for <entity>.
 * Returns DTOs only — no JPA types cross this interface.
 */
public interface <Entity>Repository {

    <Entity>ResponseDto save(/* domain creation params */);

    List<<Entity>ResponseDto> findAll();

    Optional<<Entity>ResponseDto> findById(Long id);

    // Add domain-meaningful finders. Mirror the JpaRepository methods you need,
    // but return DTOs, not entities.

    void deleteById(Long id);
}
```

Rules:
- Return type is always a DTO or `void`. Never `<Entity>JpaEntity`.
- Method names are domain language — not JPA query language.
- This interface is the seam: service code depends on this, not on the JPA repository.

---

## Step 6 — JPA adapter (repository implementation)

Create `src/main/java/com/sgarsgaya/codeatlas/repositories/<Entity>RepositoryJpaImpl.java`:

```java
package com.sgarsgaya.codeatlas.repositories;

import com.sgarsgaya.codeatlas.dto.<Entity>ResponseDto;
import com.sgarsgaya.codeatlas.dto.entity.<Entity>JpaEntity;
import com.sgarsgaya.codeatlas.mappers.<Entity>Mapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public class <Entity>RepositoryJpaImpl implements <Entity>Repository {

    private final <Entity>JpaRepository jpaRepository;
    private final <Entity>Mapper mapper;

    public <Entity>RepositoryJpaImpl(<Entity>JpaRepository jpaRepository, <Entity>Mapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public <Entity>ResponseDto save(/* params */) {
        return mapper.toDto(jpaRepository.save(new <Entity>JpaEntity(/* params */)));
    }

    @Override
    public List<<Entity>ResponseDto> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public Optional<<Entity>ResponseDto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
```

Rules:
- No business logic here — no duplicate checks, no active-state rules. Those live in the service.
- `@Transactional` on write methods only.
- Uses the injected `<Entity>Mapper` — no hand-written field mapping.

---

## Step 7 — Exception types

Create in `src/main/java/com/sgarsgaya/codeatlas/exception/`:

```java
public class <Entity>NotFoundException extends RuntimeException {
    public <Entity>NotFoundException(String identifier) {
        super("<Entity> not found: " + identifier);
    }
}
```

Add additional exceptions for domain rules (e.g. `<Entity>AlreadyExistsException`, `<Entity>InUseException`).

Register each in `GlobalExceptionHandler` with the correct HTTP status:

```java
@ExceptionHandler(<Entity>NotFoundException.class)
public ResponseEntity<ErrorResponse> handle<Entity>NotFound(<Entity>NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", ex.getMessage()));
}
```

---

## Step 8 — Service interface and implementation

Create `src/main/java/com/sgarsgaya/codeatlas/service/<Entity>Service.java`:

```java
public interface <Entity>Service {
    <Entity>ResponseDto create<Entity>(<Entity>RequestDto request);
    List<<Entity>ResponseDto> list<Entities>();
    // Add domain operations as needed
}
```

Create `src/main/java/com/sgarsgaya/codeatlas/service/impl/<Entity>ServiceImpl.java`:

```java
@Service
public class <Entity>ServiceImpl implements <Entity>Service {

    private final <Entity>Repository <entity>Repository;
    private final TimeProvider timeProvider;

    // Constructor injection only.

    @Override
    public <Entity>ResponseDto create<Entity>(<Entity>RequestDto request) {
        // Business rules live here: duplicate check, active-state transitions, etc.
        return <entity>Repository.save(/* params */);
    }
}
```

Rules:
- Inject `<Entity>Repository` (the interface), never `<Entity>RepositoryJpaImpl`.
- Inject `TimeProvider` for any `Instant.now()` call — never call `Instant.now()` directly (untestable).
- All business rules (duplicate check, state validation, authorisation) live here, not in the repository or controller.

---

## Step 9 — Controller

Follow the **openapi-first** skill (`.claude/skills/openapi-first/SKILL.md`) to add spec entries first, then create the controller:

```java
@RestController
@RequestMapping(AppConstants.API_BASE)
public class <Entity>Controller implements <Entity>Api {

    private final <Entity>Service <entity>Service;

    public <Entity>Controller(<Entity>Service <entity>Service) {
        this.<entity>Service = <entity>Service;
    }

    @Override
    public ResponseEntity<<Entity>Response> create<Entity>(<Entity>Request request) {
        <Entity>ResponseDto dto = <entity>Service.create<Entity>(new <Entity>RequestDto(request.get<Field>()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(dto));
    }

    // One private toResponse() method per entity — maps DTO → generated OpenAPI model.
    private <Entity>Response toResponse(<Entity>ResponseDto dto) {
        return new <Entity>Response()
                .id(dto.id())
                /* .field(dto.field()) */;
    }
}
```

---

## Step 10 — Tests

**Unit test** — `server/src/test/java/.../unit/service/<Entity>ServiceImplTest.java`:
- `@ExtendWith(MockitoExtension.class)` — no Spring context.
- Mock `<Entity>Repository` and `TimeProvider`.
- Cover: happy-path create, duplicate exception, not-found exception, list.

**Unit test** — `server/src/test/java/.../unit/repositories/<Entity>RepositoryJpaImplTest.java`:
- Mock `<Entity>JpaRepository` and `<Entity>Mapper`.
- Cover: save (calls mapper + jpa save), findAll (streams through mapper), findById (present + empty), deleteById.

**Integration test** — `integration-tests/src/test/java/.../integration/<Entity>ApiIntegrationTest.java`:
- Extend `IntegrationTestBase` (starts the app at `RANDOM_PORT`, wires `CodeAtlasClient`).
- Use `client.create<Entity>(...)`, `client.list<Entities>()`, etc. — real HTTP calls, no MockMvc.
- Cover: create (201 + body), duplicate (409 + `DUPLICATE_<ENTITY>` error code), list (200 + array), not-found (404 + `NOT_FOUND`).

---

## Checklist — done when all boxes are ticked

- [ ] `dto/entity/<Entity>JpaEntity.java` created
- [ ] `repositories/<Entity>JpaRepository.java` created
- [ ] `dto/<Entity>ResponseDto.java` (and `RequestDto`) created
- [ ] `mappers/<Entity>Mapper.java` created with `@Mapper(componentModel = "spring")`
- [ ] `repositories/<Entity>Repository.java` interface created (returns DTOs only)
- [ ] `repositories/<Entity>RepositoryJpaImpl.java` created (uses mapper, no business logic)
- [ ] Exception types created and registered in `GlobalExceptionHandler`
- [ ] `service/<Entity>Service.java` interface created
- [ ] `service/impl/<Entity>ServiceImpl.java` created (all business logic here)
- [ ] OpenAPI spec entries added (`openapi-first` skill followed)
- [ ] Controller created / extended (one delegation call per method)
- [ ] Unit tests: service impl + repository impl
- [ ] Integration tests: real HTTP round-trip via `CodeAtlasClient` in `integration-tests/` module
- [ ] `./gradlew test` passes
