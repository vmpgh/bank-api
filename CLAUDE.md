# CLAUDE.md

Single Spring Boot 4.1 / Java 21 service exposing banking REST APIs (accounts, deposits, withdrawals, transfers). Uses PostgreSQL (Flyway), Redis, and Kafka via a transactional outbox; deployed to AWS EKS. The app both produces and consumes its own `transfer-completed` events.

## Stack notes that affect code

- JSON is **Jackson 3**: use `tools.jackson.databind.ObjectMapper` in main code, not `com.fasterxml.jackson.databind`.
- Lombok + MapStruct (with `lombok-mapstruct-binding`); JWT via jjwt (HMAC); springdoc for OpenAPI.
- AWS SDK v2 (Secrets Manager) and `spring-cloud-aws-starter-secrets-manager`; MSK uses `aws-msk-iam-auth`.
- AWS ElastiCache runs **Valkey** (Redis-compatible) with TLS.

## Profiles

| Profile | Use |
|---|---|
| `local` | App on host; Postgres `localhost:5432`, Kafka `localhost:29092`, Redis `localhost:6379` (from `compose.yaml`) |
| `docker` | App inside Compose; datasource from `SPRING_DATASOURCE_*` env vars |
| `aws` | RDS creds from Secrets Manager (`RDS_SECRET_ARN`), JWT secret imported from `aws-secretsmanager:bank-api/jwt-secret`, MSK IAM SASL, Redis SSL |
| `test` | Testcontainers; Kafka listeners don't auto-start; topics suffixed `-test`; rate limit 5/60s |

## Architecture

Layered: **Controller → Service (interface + `impl`) → Repository → Entity**.

Request path:
1. `JwtAuthenticationFilter` parses `Authorization: Bearer <jwt>`, loads the user from the DB, sets the `SecurityContext`.
2. `@Valid` DTO validation → `@PreAuthorize` (role and/or `@accountSecurity.isOwner(...)`).
3. Service method runs in `@Transactional`, with Spring Cache (Redis) annotations.
4. Exceptions → `ApiError` JSON via `GlobalExceptionHandler`.

Transfer flow (`POST /api/v1/transfers`, requires `Idempotency-Key` header):
- Controller: Redis rate limit (`rate_limit:<user>`) → Redis idempotency reserve (`idempotency:<key>`, PROCESSING 30s → SUCCESS 24h, deleted on failure).
- `TransferServiceImpl`: debit + credit + save `OutboxEvent` **in one DB transaction**; evicts both accounts from cache.
- `OutboxScheduler` (`@Scheduled(fixedDelay = 5000)`): publishes `PENDING` events to Kafka; exponential backoff (30s × 2^(n-1)); after 5 retries → DLT and `FAILED`.
- `TransferCompletedConsumer` → `NotificationServiceImpl`, deduplicated via `processed_events`.

## Packages (`com.bank.bankapi`)

- `config` – AWS client (region hard-coded `EU_CENTRAL_1`), `aws`-profile DataSource, Kafka topics, OpenAPI, JPA auditing, seed users (`DataInitializer`).
- `controller` – `AuthController`, `AccountController`, `TransferController`, `SecretsTestController` (debug).
- `dto`, `dto.auth` – request/response DTOs with Bean Validation.
- `entity`, `persistence` – `User`, `Account`, `Role`; `AuditableEntity` (created/updated timestamps).
- `repository` – Spring Data JPA repositories.
- `mapper` – MapStruct mappers.
- `service`, `service.impl` – service interfaces/implementations; `AwsSecretsManagerService`.
- `security.*` – JWT filter/service, `UserDetailsService`, `SecurityConfig`, `AccountSecurity` (ownership).
- `outbox` – outbox entity, repository, service, scheduler.
- `kafka.event` / `kafka.producer` / `kafka.consumer` – event records, main + DLT publishers, listener.
- `notification` – idempotent event handling.
- `redis.cache`, `redis.imdepotency`, `redis.ratelimit`, `redis.lock` – cache manager, idempotency, rate limiting, distributed lock. (`imdepotency` is misspelled; don't rename unless asked.)
- `metrics` – Micrometer counters (`BankMetrics`, `KafkaMetrics`, `CacheMetrics`).
- `exception` – custom exceptions, `ApiError`, `GlobalExceptionHandler`.

## Coding conventions

- Constructor injection via Lombok `@RequiredArgsConstructor` + `private final` fields; `@Slf4j` for logging.
- Services: interface + `*Impl`; Redis-backed implementations named `Redis*`.
- `@Transactional` on service methods; `@Transactional(readOnly = true)` for reads.
- Domain rules live on entities (`Account.deposit/withdraw`, `OutboxEvent.markForRetry/markPublished/markFailed`).
- UUID primary keys (`GenerationType.UUID`); money is `BigDecimal` / `NUMERIC(19,2)` — never `double`.
- Accounts are soft-deleted (`deleted`, `deletedAt`); `Account` has `@Version` optimistic locking.
- Events and small value types are Java `record`s.
- REST paths under `/api/v1/...`; `CREATED` for create, `NO_CONTENT` for delete; `@SecurityRequirement(name = "bearerAuth")` on protected controllers.
- Cache name `"accounts"` keyed by account UUID; any balance-changing method must evict affected account keys.
- Instrument key operations with `@Observed(name = "bank.<area>.<action>")`; business counters go in `BankMetrics` / `KafkaMetrics`.
- New exceptions go in `exception/` with a matching `@ExceptionHandler` returning `ApiError`.
- App-specific properties use the `bank.*` prefix, read with `@Value`.
- Commits use Conventional Commit prefixes (`feat:`, `test:`).

## Database and migrations

- Schema is owned by **Flyway**; Hibernate uses `ddl-auto=validate`, so entities must match the migrated schema exactly.
- Migrations: `src/main/resources/db/migration/V<n>__<snake_case_description>.sql` (latest: `V6`). Never edit an applied migration — add `V<n+1>`.
- Tables: `users`, `accounts`, `outbox_events`, `processed_events`. `users`/`accounts` use `TIMESTAMP WITH TIME ZONE`; `outbox_events`/`processed_events` use `TIMESTAMP` — match the existing table when altering it.
- If integration tests write to a new table, add it to `src/test/resources/sql/cleanup.sql` (`users` is intentionally not truncated).

## Security requirements

- Stateless JWT (`SessionCreationPolicy.STATELESS`), CSRF disabled, BCrypt passwords. Roles `USER`/`ADMIN` granted as `ROLE_<name>`.
- Account-scoped endpoints must use:
  `@PreAuthorize("hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)")`
- Public routes are defined in `SecurityConfig.PUBLIC_ENDPOINTS`; don't widen it without an explicit request.
- `jwt.secret` in `application.properties` is development-only. Never commit real secrets to source, properties, or k8s manifests; AWS secrets come from Secrets Manager via EKS Pod Identity.
- `DataInitializer` seeds `admin`, `john`, `alice` (password `password`) in `local`, `docker`, `test`, and `aws`; integration tests depend on them.

## Build and test

```bash
mvn clean verify                     # what CI runs: compile + all tests
mvn test -Dtest=AccountServiceImplTest
mvn clean package -DskipTests        # builds target/bank-api-0.0.1-SNAPSHOT.jar
```

- A Maven wrapper (`mvnw`) is tracked, but `.mvn/wrapper/maven-wrapper.jar` is gitignored.
- **Docker must be running** — integration and e2e tests use Testcontainers (Postgres 17, Redis 7, Kafka 4.0.0).
- No surefire/failsafe split: unit, integration and e2e tests all run in the `test` phase.
- Unit tests (`unit/`): `@ExtendWith(MockitoExtension.class)`, grouped with `@Nested`.
- Integration tests (`integration/`) extend `BaseIntegrationTest`: `@SpringBootTest` + MockMvc, `@ActiveProfiles("test")`, real login for JWTs with `admin()`/`john()`/`alice()` helpers, `cleanup.sql` before each test.
- New service logic needs Mockito unit tests; new endpoints need integration tests covering admin, owner and non-owner access plus validation.

## Running locally

```bash
# Infra in Compose, app on host (profile: local)
docker compose up -d postgres-db kafka redis
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Everything in Compose (profile: docker) — Dockerfile copies the prebuilt jar
mvn clean package -DskipTests && docker compose up -d
```

Compose reads `POSTGRES_*` and `SPRING_DATASOURCE_*` from a `.env` file that is gitignored and not in the repo. Swagger UI: `/swagger-ui.html`. Postman collections: `docs/postman/`.

## Deployment

- **Jenkins** (`Jenkinsfile`): `mvn clean verify` → `docker build` → push to ECR `bank-api/backend:${BUILD_NUMBER}` (eu-central-1). There is **no deploy stage**.
- **Kubernetes** (`k8s/`, namespace `bank-api`, service account `bank-api`): `deployment.yaml` pins an image tag — update it and apply manually. Env: `SPRING_PROFILES_ACTIVE=aws`, `RDS_SECRET_ARN`, `MSK_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `REDIS_PORT`. ALB Ingress lives at repo root: `bank-api-ingress.yaml`.
- **Terraform** (`infrastructure/terraform`, eu-central-1, prefix `bank-api-prod`): `bootstrap/` creates the S3 state bucket; root module wires `networking`, `ecr`, `eks`, `rds`, `msk`, `redis`. These are real AWS resources — run `terraform plan` and get user confirmation before any `apply` or `destroy`.
- The EKS Pod Identity association / IAM roles are not defined in Terraform.

## Known gaps (do not assume these work)

- `RedisDistributedLockService` is unused; `OutboxScheduler` is not safe with more than one replica.
- Only `getById` filters soft-deleted accounts (`findByIdAndDeletedFalse`); deposit, withdraw, transfer and `AccountSecurity.isOwner` use `findById`.
- `GlobalExceptionHandler` doesn't handle `OptimisticLockingFailureException`, missing request headers, or access-denied; invalid/expired JWTs throw inside the filter.
- `TransferCompletedConsumer` hard-codes `topics = "transfer-completed"` instead of using `bank.kafka.topics.transfer-completed`.
- `IdempotencyServiceImpl.get` calls `key.substring(0, 8)` (unused) and throws for keys shorter than 8 chars.
- `metrics/MetricsRedisCache.java` is fully commented out.
- `/actuator/**` is public, and the Ingress exposes observability UIs over HTTP :80.
- `k8s/secret.yaml` is empty.
