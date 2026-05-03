# Prepare PR

Produce a pull request that is merge-ready from the first review.
This skill runs through every pre-PR gate in order and blocks on failures.
Do not open a PR until every section below is green.

## When to use

Trigger this skill when:
- You are about to open a pull request
- You hear "prepare PR", "open a PR", "ship this", "create a pull request"
- After completing a feature or fix and running `java-staff-review`

---

## Step 1 — Confirm the branch is clean

```bash
git status          # no uncommitted changes
git diff origin/main --stat  # review what will be in the PR
```

- All changes are committed.
- No untracked files that should be in the PR.
- No debug code, `System.out.println`, `e.printStackTrace()`, or `TODO` without an issue reference.

---

## Step 2 — Run the full staff review

Read and apply `.claude/skills/java-staff-review/SKILL.md` against all changed files.

A PR cannot be opened if any **BLOCKER** finding exists from the staff review.
Resolve all BLOCKERs, then continue.

---

## Step 3 — Build and test gate

```bash
./gradlew clean build
```

Must produce:
- `:openapi-spec:openApiGenerate` — SUCCESS
- `compileJava` — SUCCESS (zero warnings treated as errors if `-Xlint` is active)
- `test` — SUCCESS, zero failures, zero skipped tests that were previously passing

If tests fail: fix them before opening the PR. Do not open a PR with a red build.

---

## Step 4 — Dead code check

Read and apply `.claude/skills/dead-code-removal/SKILL.md` scoped to the changed files.

Remove any dead code introduced or uncovered by this change before the PR is opened.

---

## Step 5 — Self-review the diff

```bash
git diff origin/main
```

Walk every changed file and ask:

| Question | Gate |
|---|---|
| Does every new public method have a unit test? | BLOCKER if no |
| Does every new/changed endpoint have a test in `integration-tests/` using `CodeAtlasClient`? | BLOCKER if no |
| Are all new exception paths mapped in `GlobalExceptionHandler`? | BLOCKER if no |
| Is any file larger than 200 lines? | WARNING — consider splitting |
| Are there any hand-written field mappings that MapStruct should own? | WARNING |
| Does any controller method contain business logic? | BLOCKER if yes |
| Does any service method import a JPA entity type? | BLOCKER if yes |
| Is any method longer than 20 lines? | WARNING — consider extracting |

---

## Step 6 — Write the PR description

Use this template. Every section is required — no empty sections.

```markdown
## Problem

[One paragraph: what was broken, missing, or sub-optimal before this change.
Be specific — name the symptom, not the solution. E.g. "Snapshot creation leaked the JPA
entity type into the service layer, making the domain boundary impossible to enforce."]

## Explored solutions

[Bullet list of alternatives seriously considered, with one-line reason each was rejected.
Omit this section only if there was exactly one viable approach.

- **Option A — [name]**: rejected because [reason]
- **Option B — [name]**: rejected because [reason]
]

## Adapted approach

[One paragraph: what was chosen and why. Reference the relevant skill if a pattern was followed
(e.g. "Followed `.claude/skills/new-domain-entity/SKILL.md` for the `GraphIndex` entity").
Explain any deliberate trade-offs made.]

## Blast radius

[List every layer touched and classify the change scope.]

| Layer | Changed? | Notes |
|---|---|---|
| OpenAPI spec (`openapi-spec/`) | yes / no | |
| Generated interfaces / models | yes / no | |
| Controller | yes / no | |
| Service | yes / no | |
| Repository | yes / no | |
| Mapper | yes / no | |
| Tests | yes / no | |

**Overall scope:** contained / moderate / wide

## Code coverage

- **Unit tests added/updated:** [list classes, e.g. `SnapshotServiceImplTest`, `SnapshotRepositoryJpaImplTest`]
- **Integration tests added/updated:** [list classes in `integration-tests/` module that exercise the endpoint via `CodeAtlasClient`]
- **Lines / branches covered by new tests:** [best-effort summary, e.g. "happy path + duplicate name + not-found"]
- **Manually verified:** [what you ran and what you saw — e.g. `./gradlew clean build` output, curl, Postman]

## Checklist

- [ ] `./gradlew clean build` passes
- [ ] Staff review passed (no BLOCKERs)
- [ ] Dead code removed
- [ ] No business logic in controllers
- [ ] No JPA types outside `repositories/`
- [ ] All new endpoints have integration tests
- [ ] PR description complete
```

---

## Step 7 — Open the PR

```bash
git push -u origin HEAD

gh pr create \
  --title "<type>(<scope>): <imperative summary>" \
  --body "$(cat <<'EOF'
[paste PR description here]
EOF
)"
```

**Title format:** `<type>(<scope>): <summary>`
- Types: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`
- Scope: the primary package or subsystem changed (`snapshots`, `graph`, `openapi-spec`, `ci`)
- Summary: imperative, max 72 chars, no period

Examples:
- `feat(snapshots): add delete and active-snapshot endpoints`
- `refactor(repositories): replace domain type with MapStruct mapper`
- `fix(exception-handler): return 409 for duplicate snapshot name`

---

## Step 8 — Post-open checks

After the PR is open:

```bash
gh pr view --web    # confirm CI is triggered
gh pr checks        # wait for CI to go green
```

If CI fails on a check that passed locally, investigate before asking for review. Do not request review on a red PR.

---

## Anti-patterns

- Do not open a PR with failing tests and a note "tests will be fixed later".
- Do not open a PR with a title like "WIP" or "fixes".
- Do not open a PR without a description body.
- Do not open a PR for a change that mixes unrelated concerns — split it.
