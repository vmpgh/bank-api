---
name: spring-code-review
description: Reviews Java and Spring Boot code for correctness, security vulnerabilities, performance, maintainability, and architectural problems. Use when asked to review, audit, or evaluate backend code.
---

# Spring Code Review

A read-only review procedure for this repository's Java 21 / Spring Boot 4.1 banking service (PostgreSQL + Flyway, Redis/Valkey, Kafka transactional outbox, JWT auth). `CLAUDE.md` is the source of truth for project conventions; this skill tells you how to apply them when reviewing.

## Ground rules

- **Read-only.** Do not create, edit, stage, or commit files. Do not run destructive commands (`git reset`, `git checkout -- …`, `rm`, `docker compose down -v`, `mvn clean` on a dirty tree, etc.).
- **No deployment, no production access.** Do not run `terraform apply/destroy`, `kubectl apply/delete`, `docker push`, or any `aws` CLI call against real accounts. Do not use the `aws` Spring profile.
- Allowed: reading files, `git diff` / `git log` / `git show` / `git status`, `grep`/`find`, and — only if the user asks — `mvn test` or `mvn -q compile` (these need Docker for Testcontainers; say so if Docker isn't running rather than guessing at results).
- **Inspect before reporting.** For every finding, read the code involved *and* what it depends on: the callers, the entity, the repository query, the migration that defines the column, the security expression, the test that should cover it. A finding based on one file in isolation is usually wrong.
- **Separate confirmed from potential.** A *confirmed* problem is one you can trace to a concrete failing input or state. A *potential risk* depends on assumptions (load, deployment topology, future callers). Label each finding accordingly.
- **No style nitpicks** unless they materially hurt maintainability (e.g., a misleading name that hides behavior). Formatting, import order, and personal preference are out of scope.
- Do not re-report items already listed under **Known gaps** in `CLAUDE.md` as new discoveries. Mention one only if the code under review makes it worse or depends on it working.

## Step 1 — Establish scope

1. Determine what to review:
   - A diff: `git diff`, `git diff --cached`, or `git diff main...HEAD`.
   - A PR/branch, specific files, or a package the user named.
   - If no target is given, ask, or default to the current uncommitted + branch changes.
2. List the changed/target files and group them by layer: controller, DTO, service/impl, repository, entity, migration, security, outbox/kafka, redis, config, tests.
3. For each file, identify its collaborators and open them. At minimum follow: controller → service interface → impl → repository → entity → Flyway migration.

## Step 2 — Review checklist

Work through every section relevant to the scope. Project-specific checks are marked **(project)**.

### 1. Correctness and potential bugs

- Null handling: `Optional` unwrapped with `.get()`, unchecked repository results, nullable DTO fields used without `@NotNull`.
- Money: `BigDecimal` only, never `double`/`float`; comparisons use `compareTo`, not `equals` (scale-sensitive); explicit `RoundingMode` on division; scale matches `NUMERIC(19,2)`. **(project)**
- Domain rules enforced on the entity (`Account.deposit/withdraw`, `OutboxEvent.mark*`), not duplicated or bypassed in services. **(project)**
- Soft deletes: does new code that loads an `Account` use `findByIdAndDeletedFalse`, or can it act on a deleted account? **(project)**
- Off-by-one and boundary cases: zero/negative amounts, self-transfer (source == target), identical idempotency keys, empty collections.
- Exceptions swallowed or logged-and-ignored; checked exceptions wrapped without cause.
- `equals`/`hashCode` on JPA entities (Lombok `@Data` on entities is a bug risk with lazy relations and generated IDs).
- Records used for events and small value types; mutability where immutability is expected. **(project)**
- Jackson 3: main code must import `tools.jackson.databind.*`, not `com.fasterxml.jackson.databind.*`. **(project)**

### 2. SOLID and maintainability

- Single responsibility: controllers doing business logic, services doing HTTP/transport concerns, entities calling repositories.
- Services follow interface + `*Impl`; Redis-backed implementations named `Redis*`. **(project)**
- Depend on interfaces, not implementations, across layers.
- Duplicated logic that should live in one place (an entity method, a mapper, a shared validator).
- MapStruct used for entity↔DTO mapping instead of hand-written copying; mappers don't leak entities out of the service layer.
- Dead code, commented-out classes, or unused beans that mislead readers.

### 3. Spring DI and architecture

- Constructor injection via `@RequiredArgsConstructor` + `private final`; no field `@Autowired`. **(project)**
- Layering: Controller → Service → Repository → Entity. Controllers must not call repositories directly.
- Self-invocation: a `@Transactional`, `@Cacheable`, `@CacheEvict`, `@PreAuthorize`, or `@Observed` method called from inside the same bean bypasses the proxy and silently does nothing.
- Bean scope and statefulness: singleton beans holding mutable per-request state.
- Profile-specific beans (`@Profile("aws")`, etc.) and whether the `test` profile still wires correctly.
- Configuration: app-specific properties use the `bank.*` prefix and `@Value`; no hard-coded values that differ per environment (topic names, regions, TTLs). **(project)**

### 4. Transaction boundaries and database consistency

- `@Transactional` on service methods that write; `@Transactional(readOnly = true)` on reads. **(project)**
- Transfers: debit, credit, and `OutboxEvent` insert must happen in **one** transaction. Any change that splits them, or publishes to Kafka directly inside the transaction, breaks the outbox guarantee. **(project)**
- Side effects that can't be rolled back (Kafka sends, Redis writes, HTTP calls) inside a DB transaction — what happens if the commit fails afterward?
- Optimistic locking: `Account` has `@Version`. Check whether `OptimisticLockingFailureException` can surface and how it is handled (retried, mapped to 409, or leaking as 500). **(project)**
- Lock ordering for multi-row updates (transfers between A→B and B→A concurrently) if pessimistic locks are introduced.
- `rollbackFor` / checked exceptions not rolling back; `REQUIRES_NEW` misuse.
- Schema changes: only via a new Flyway migration `V<n+1>__<snake_case>.sql`; never an edit to an applied migration. Entity mappings must match the migrated schema exactly (`ddl-auto=validate`). Timestamp type must match the table (`TIMESTAMP WITH TIME ZONE` for `users`/`accounts`, `TIMESTAMP` for `outbox_events`/`processed_events`). **(project)**
- New tables written by integration tests are added to `src/test/resources/sql/cleanup.sql`. **(project)**

### 5. JPA / Hibernate performance

- N+1 queries: lazy associations accessed in a loop, in a mapper, or during JSON serialization. Look for collection getters inside `stream().map(...)` and MapStruct mappings of nested entities. Suggest `@EntityGraph`, `JOIN FETCH`, or a DTO projection.
- `FetchType.EAGER` on `@ManyToOne`/`@OneToMany` loading more than needed.
- Unbounded queries: `findAll()` without paging on tables that grow (`accounts`, `outbox_events`, `processed_events`). Prefer `Pageable` / `Slice` / limited queries.
- Missing indexes for new query predicates — check the migrations for an index on the filtered/sorted columns.
- Loading an entity just to check existence (`existsBy…` instead), or loading full entities for counts.
- Bulk updates via loops of `save()` instead of a batch or `@Modifying` query (and the `@Modifying(clearAutomatically = true)` pitfall).
- Open-session-in-view reliance: lazy loading from the controller layer.

### 6. REST API design and exception handling

- Paths under `/api/v1/...`; resource-oriented naming; correct verbs. **(project)**
- Status codes: `201 CREATED` for create, `204 NO_CONTENT` for delete, `404` for missing, `409` for conflicts/idempotency/optimistic-lock, `422`/`400` for validation, `429` for rate limits. **(project)**
- Request DTOs annotated with Bean Validation and the controller parameter has `@Valid`.
- Entities never returned from or accepted by controllers — DTOs only.
- New exceptions live in `exception/` with a matching `@ExceptionHandler` in `GlobalExceptionHandler` returning `ApiError`. **(project)**
- Error responses do not leak stack traces, SQL, internal IDs of other users, or secret values.
- Idempotency: endpoints that move money require `Idempotency-Key`; check reservation/release on both success and failure paths. **(project)**
- OpenAPI: protected controllers carry `@SecurityRequirement(name = "bearerAuth")`. **(project)**

### 7. Authentication and authorization

- Account-scoped endpoints use exactly:
  `@PreAuthorize("hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)")`
  and the `#id` parameter name matches the method parameter. **(project)**
- IDOR: any endpoint that accepts an account/user ID in the path, body, or query must check ownership — including the *target* side of operations and IDs nested in request bodies (e.g., transfer source account).
- `SecurityConfig.PUBLIC_ENDPOINTS` not widened without an explicit request. **(project)**
- JWT: signature verification, expiry check, algorithm pinned (HMAC), no secret logged. `jwt.secret` in `application.properties` is dev-only; no real secrets in source, properties, or k8s manifests. **(project)**
- Passwords hashed with BCrypt; no plaintext passwords in logs or responses.
- Role checks use `hasRole('X')` against authorities `ROLE_X`. **(project)**
- Sensitive data in logs (`@Slf4j` calls printing tokens, full request bodies, balances with PII).
- Actuator and debug endpoints (`SecretsTestController`) not newly exposed or expanded.

### 8. Concurrency and performance

- Lost updates on balances: concurrent deposit/withdraw/transfer on the same account — is `@Version` enforced and is the failure handled?
- Redis rate limiting and idempotency: atomicity of check-then-set (use `SET NX EX` / Lua, not `GET` then `SET`); TTL always set; key cleanup on failure. **(project)**
- Cache coherence: every balance-changing method evicts the affected `"accounts"` cache keys (both accounts in a transfer); no caching of data that is about to change in the same transaction; eviction happens after commit where it matters. **(project)**
- Scheduled jobs (`OutboxScheduler`) under multiple replicas: duplicate publishing, missing distributed lock / `SELECT … FOR UPDATE SKIP LOCKED`. **(project)**
- Kafka consumer idempotency via `processed_events`; at-least-once delivery assumptions; DLT handling; topic names from `bank.kafka.topics.*` rather than literals. **(project)**
- Blocking I/O on request threads where virtual threads or async would matter; thread-pool sizing; unbounded in-memory collections.
- Observability: key operations annotated `@Observed(name = "bank.<area>.<action>")`; business counters in `BankMetrics` / `KafkaMetrics`. **(project)**

### 9. Tests (JUnit 5 + Mockito)

- New service logic has unit tests under `unit/` using `@ExtendWith(MockitoExtension.class)` and `@Nested` grouping. **(project)**
- New endpoints have integration tests under `integration/` extending `BaseIntegrationTest`, covering **admin, owner, and non-owner** access plus validation failures. **(project)**
- Tests assert behavior, not just "no exception": balances after operations, persisted outbox rows, cache eviction, HTTP status *and* `ApiError` body.
- Edge cases covered: insufficient funds, zero/negative amounts, deleted accounts, duplicate idempotency key, rate limit exceeded, concurrent modification.
- Mockito hygiene: no `any()` where a specific argument matters; `verify` used for side effects; no over-mocking of value objects or entities; strict stubs not silenced with `lenient()` without reason.
- No reliance on test ordering or shared mutable state; seeded users `admin`/`john`/`alice` used via helpers, not re-created. **(project)**
- Testcontainers dependency noted — tests that would need Docker should not be reported as "failing" if Docker isn't available.

## Step 3 — Verify each finding

Before writing a finding down:

1. Re-read the exact lines and confirm the line numbers.
2. Trace at least one concrete path that triggers it (input, state, or call sequence). If you can't, downgrade it to a potential risk or drop it.
3. Check whether something elsewhere already handles it (a filter, an aspect, a DB constraint, a handler, a test).
4. Check `CLAUDE.md` **Known gaps** — if it's already listed, don't present it as new.

## Step 4 — Severity

| Severity | Meaning | Examples |
|---|---|---|
| **Critical** | Money can be lost/created, auth bypass, data corruption, secret exposure | Missing ownership check on a money-moving endpoint; debit and credit in separate transactions; real secret committed |
| **High** | Incorrect results or outages under realistic conditions | Lost update on concurrent withdrawals; cache not evicted after balance change; migration that fails `validate` |
| **Medium** | Bugs in edge cases, performance problems at scale, missing required tests | N+1 on a list endpoint; unhandled exception returning 500; new endpoint without non-owner test |
| **Low** | Maintainability issues with real cost, minor inefficiencies | Hard-coded config value; duplicated validation logic; misleading name |

## Step 5 — Report format

Order findings by severity (Critical → Low). For each:

```
### [Severity] Short title
- **Status:** Confirmed | Potential risk
- **Location:** `path/to/File.java:123` (add more locations if relevant)
- **Area:** Correctness | SOLID | DI/Architecture | Transactions | JPA | REST | Security | Concurrency | Tests
- **Problem:** What is wrong, in one or two sentences.
- **Why it matters:** The concrete consequence (who is affected, what breaks, under what conditions).
- **Trigger:** The input/state/sequence that causes it (required for Confirmed).
- **Suggested fix:** A practical change — show a short code snippet when it clarifies the fix.
```

If a section of the checklist had no findings, don't pad the report; list the areas you checked in the summary instead.

## Step 6 — Summary

End with a short summary (5–10 lines):

- Scope reviewed (files/diff/branch) and anything you could not inspect.
- Count of findings by severity, split into confirmed vs. potential.
- The one or two issues to fix first.
- Overall assessment: ready to merge / merge after fixes / needs rework.
- Any tests or checks you recommend running (e.g., `mvn test -Dtest=…`), noting that integration tests require Docker.
