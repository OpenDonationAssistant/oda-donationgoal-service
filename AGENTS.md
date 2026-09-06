# ODA DonationGoal Service — Agent Instructions

## What This Is

Java 25 / Micronaut 5.1.0 microservice managing donation goals for streamers. **Purely event-driven** — no HTTP controllers. All I/O flows through RabbitMQ queues. Part of the Open Donation Assistant ecosystem.

## Build & Test Commands

```bash
# Build JAR (default)
mvn clean package

# Build native image (CI path — what Docker actually runs)
mvn clean package -Dpackaging=native-image

# Run tests
mvn test
mvn verify  # includes integration tests if any

# Run locally
mvn mn:run  # Uses "standalone" profile (hardcoded)

# Sonar analysis (needs SONAR_TOKEN)
mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

**No mvnw** — use system `mvn`. No separate lint script; static analysis runs at compile time.

## Critical Gotchas

### Build

- **JVM flags required**: `.mvn/jvm.config` injects `--add-exports`/`--add-opens` for annotation processors. Running `javac` outside Maven without these flags will fail.
- **Native binary location**: Dockerfile expects `target/oda-donationgoal-service` (no extension). Plain `mvn package` (jar) won't produce a runnable Docker image.
- **AOT is effectively off**: `micronaut.aot.enabled=false` in pom.xml despite AOT config files existing.

### Code

- **NullAway enforced**: `NullAway:ERROR` via Error Prone. Base package is `@NullUnmarked` — unannotated third-party types bypass checks. Use `@Nullable` markers in handlers/repos.
- **Command dispatch is manual**: `CommandListener.listen` uses a switch on `type` header. Adding new commands requires extending that switch.
- **No transactions**: Multi-step mutations (e.g., `GoalListener.listenFinished`) do several repository writes without transaction boundaries. Comments note "TODO: batch".
- **Russian comments**: Mixed-language codebase (Russian comments in `GoalListener`).
- **Money handling rough**: DB columns are `varchar(255)`. `Goal.update()` hardcodes currency RUB. See `// TODO: use Amount as is`.

### Testing

- **Unit tests only**: Pure Mockito with Instancio `@Given` fixtures. No DB/containers.
- **No integration tests**: Despite `micronaut.test.resources.enabled=true` in config.
- **Test files**: `PaymentEventHandlerTest`, `HistoryItemEventHandlerTest` in `src/test/java/.../handlers/`

## Architecture

```
RabbitMQ queues → Listeners → EventHandlers → Facades (from oda-rabbit-conf lib)
                                                      ↓
                                              PostgreSQL (donationgoal schema)
```

**Key components**:
- `Application.java`: Entrypoint, registers RabbitMQ exchanges
- `EventsListener`, `ConfigListener`, `CommandListener`, `GoalListener`: RabbitMQ consumers
- `Goal.java`: Domain aggregate (add/decrease amounts, save, delete)
- `GoalData.java`, `GoalLink.java`: JPA entities
- Facades (`GoalFacade`, `HistoryFacade`) come from `oda-rabbit-conf` library

**Single-threaded consumers**: Both `events-listener` and `command-listener` executors are fixed `nThreads: 1` — ordering is intentional.

## Environment Variables

| Variable | Default | Required |
|----------|---------|----------|
| `RABBITMQ_HOST` | `localhost` | Yes |
| `JDBC_URL` | `jdbc:postgresql://localhost/postgres` | Yes |
| `JDBC_USER` | `postgres` | Yes |
| `JDBC_PASSWORD` | `postgres` | Yes |
| `JWKS_URI` | **none** | **Critical** — JWT validation won't work without it |

**Profile**: `standalone` is hardcoded as default. An `allinone` profile exists for local dev with different datasource config.

## CI/CD

- **Single workflow**: `.github/workflows/maven.yml` triggers on push to `master`
- **Delegates to**: `OpenDonationAssistant/oda-libraries/.github/workflows/release_service.yml@master`
- **Builds native image** → Docker → pushes to `ghcr.io/opendonationassistant/oda-donationgoal-service`
- **Version**: `${{ github.RUN_NUMBER }}` (not semantic versioning)

## Important Files

- `pom.xml`: Dependencies, processors, Sonar config, AOT settings
- `src/main/resources/application.yml`: Security, executors, serde, flyway
- `src/main/resources/application-standalone.yml`: Local dev defaults
- `.mvn/jvm.config`: Required JVM flags for annotation processors
- `.github/workflows/maven.yml`: CI/CD definition
- `Dockerfile`: Native image deployment

## Conventions

- **JSpecify nullability**: Use `@Nullable` on types that can be null
- **RabbitMQ ack**: Manual acknowledgment in `EventsListener` via `RabbitAcknowledgement`
- **Idempotency**: `goal_link` table deduplicates payment→goal mappings via time-based UUIDs
- **Widget sync**: Goals are synced to payment page config via `ConfigCommand.PutKeyValue`
- **Sonar typo**: `pom.xml` has `sonar.proectKey` (misspelled) — ignore it

## What NOT to Do

- Don't add HTTP controllers — this is an event-driven service
- Don't assume jar packaging works in Docker — always use `-Dpackaging=native-image`
- Don't skip `@Nullable` annotations — NullAway will fail the build
- Don't modify `CommandListener.listen` switch without adding proper command classes
- Don't assume transactions exist — they don't for multi-step operations
- Don't run without `JWKS_URI` — auth will break silently

## Context Sources

- ODA libraries: `io.github.opendonationassistant:oda-rabbit-conf:${oda.version}` (0.11.232)
- Reusable CI workflows: `OpenDonationAssistant/oda-libraries/.github/workflows/`
- Docker image: `ghcr.io/opendonationassistant/oda-donationgoal-service`

---

*Last verified: 2026-09-05*
