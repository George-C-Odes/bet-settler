# Repository Structure

`bet-settler` uses a pragmatic ports-and-adapters layout with a single Spring Boot runtime module.

## Top-level layout

| Path                 | Purpose                                                                                                                       |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `src/main/java`      | Application source code split into `domain`, `application`, and `infrastructure` layers                                       |
| `src/main/resources` | Spring configuration, Flyway migrations, and profile-specific runtime settings                                                |
| `src/test/java`      | Unit, slice, integration, and end-to-end tests                                                                                |
| `.run`               | Checked-in IntelliJ run configurations for host-run profiles, Docker Compose flows, and test-stat refresh                     |
| `compose.yaml`       | Local Docker stack for Kafka, RocketMQ, dashboards, and the app container                                                     |
| `pom.xml`            | Build, dependency, quality-gate, coverage, and Javadoc configuration                                                          |
| `config/quality`     | PMD rules and the compact-constructor Javadoc validation helper                                                               |
| `utils/test-stats`   | Small helper script and committed summary of current test/coverage stats, aligned with the JaCoCo package scopes in `pom.xml` |
| `target`             | Maven build outputs and generated reports                                                                                     |

## Layering rule

Dependency direction is intentionally one-way:

```text
infrastructure -> application -> domain
```

- `domain` contains the business model and settlement rules.
- `application` contains use-case orchestration and port contracts.
- `infrastructure` contains Spring, HTTP, messaging, persistence, configuration, and observability adapters.

No direct layer breach is present in the current main source set.

## Package map

Main package root: `io.github.georgecodes.betsettler`

```text
src/main/java/io/github/georgecodes/betsettler
├─ BetSettlerApplication.java
├─ domain/
│  ├─ model/
│  ├─ validation/
│  └─ service/
├─ application/
│  ├─ dto/
│  ├─ port/
│  │  ├─ in/
│  │  └─ out/
│  ├─ service/
│  │  └─ dispatch/
│  └─ support/
└─ infrastructure/
   ├─ config/
   ├─ messaging/
   │  ├─ kafka/
   │  │  ├─ consumer/
   │  │  ├─ dto/
   │  │  ├─ mapper/
   │  │  └─ producer/
   │  ├─ settlement/
   │  │  ├─ dto/
   │  │  ├─ mapper/
   │  │  ├─ payload/
   │  │  └─ publisher/
   │  └─ rocketmq/
   │     └─ publisher/
   ├─ observability/
   ├─ persistence/
   │  ├─ adapter/
   │  ├─ entity/
   │  ├─ mapper/
   │  └─ repository/
   └─ rest/
      ├─ controller/
      ├─ dto/
      ├─ error/
      └─ mapper/
```

## What belongs in each layer

### `domain`

Pure business concepts with no Spring, JPA, Kafka, or RocketMQ dependencies.

- `domain.model.Bet` - persisted betting data in domain form
- `domain.model.EventOutcome` - outcome received from the API/Kafka flow
- `domain.model.BetSettlement` - settlement decision aggregate
- `domain.model.SettlementResult` - `WIN` / `LOSE`
- `domain.validation.ValidationSupport` - reusable non-framework validation helpers shared across domain-adjacent types
- `domain.service.BetSettlementDecider` - settlement rule comparing predicted winner with actual winner

### `application`

Use cases, DTOs, and port abstractions that coordinate the domain and external adapters.

#### DTOs

- `application.dto.PublishEventOutcomeCommand`
- `application.dto.DemoResetSummary`
- `application.dto.SettlementDispatchRetrySummary`

#### Inbound ports

- `PublishEventOutcomeUseCase`
- `ProcessEventOutcomeUseCase`
- `GetBetsQueryUseCase`
- `ResetDemoStateUseCase`
- `RetrySettlementDispatchUseCase`

#### Outbound ports

- `EventOutcomePublisherPort`
- `BetQueryPort`
- `ProcessedEventOutcomePort`
- `SettlementAuditPort`
- `BetSettlementPayloadPort` - exposes the destination topic and serialized payload snapshot generation needed by the transactional preparation stage
- `BetSettlementDispatchPort` - publishes freshly prepared settlements to the configured outbound destination
- `BetSettlementReplayPort` - replays stored payload snapshots to the stored destination during manual retry flows
- `DemoStateResetPort`
- `SettlementFlowMetricsPort`

#### Application model records

- `model.audit.PendingSettlementAudit` - explicit batch descriptor for pending settlement-audit persistence
- `model.audit.RetryableSettlementAudit` - explicit retry descriptor for replayable `PENDING` / `FAILED` settlement audits
- `model.dispatch.PreparedDispatchPlan` - immutable duplicate/no-match/dispatch-required result of the transactional settlement-preparation stage without sentinel destination values

#### Services

- `PublishEventOutcomeService` - accepts validated publish commands and only records the accepted metric after Kafka acknowledges the handoff
- `ProcessEventOutcomeService` - orchestrates transactional preparation, publication, audit updates, metrics, and delegates failure-reason formatting to `SettlementFailureReasonFormatter`
- `SettlementDispatchPreparationService` - prepares dedup state through one insert-first processed-outcome persistence path, settlement decisions, destination-topic and payload-snapshot creation through `BetSettlementPayloadPort`, and batched pending audit rows inside the transactional stage
- `GetBetsQueryService` - loads all bets or event-filtered bets
- `ResetDemoStateService` - clears transient demo state while preserving Flyway-seeded bets
- `RetrySettlementDispatchService` - replays `PENDING` and `FAILED` settlement audit rows through the dedicated settlement replay port using the stored destination topic and payload snapshot unchanged

#### Support

- `support.SettlementFailureReasonFormatter` - creates bounded failure reasons for audit persistence during publish and replay failures

### `infrastructure`

Framework-facing adapters and configuration.

#### `infrastructure.rest`

- `controller.EventOutcomeController` - `POST /api/v1/event-outcomes`
- `controller.BetQueryController` - `GET /api/v1/bets`
- `controller.DemoController` - `POST /api/v1/internal/demo/reset` and `POST /api/v1/internal/demo/retry-settlements` in local/test-oriented profiles only
- `mapper.EventOutcomeRestMapper` - maps event-outcome request and accepted-response payloads
- `mapper.BetResponseMapper` - maps bet-query domain results into REST response payloads
- `mapper.DemoResponseMapper` - maps demo reset and retry summaries into REST response payloads
- `error.ApiExceptionHandler` - central request/validation error responses

#### `infrastructure.messaging.kafka`

- `producer.KafkaEventOutcomePublisher` - publishes accepted outcomes to Kafka and waits for broker acknowledgement within the configured timeout
- `consumer.KafkaEventOutcomeConsumer` - consumes from `event-outcomes` and triggers processing
- transport DTO and mapper classes isolate Kafka payload shape from the domain model

#### `infrastructure.messaging.rocketmq`

- `publisher.RocketMqBetSettlementPublisher` - real RocketMQ publisher used in `local-docker`, including payload-snapshot replay support

#### `infrastructure.messaging.settlement`

- `dto.BetSettlementMessage` - shared settlement message contract used for outbound publication and replay snapshots
- `mapper.BetSettlementMessageMapper` - maps domain settlements into the shared settlement message contract
- `payload.BetSettlementPayloadSnapshotFactory` - creates the destination topic and serialized payload snapshots needed by the transactional preparation stage
- `payload.BetSettlementPayloadCodec` - maps shared settlement messages to and from stored JSON payload snapshots for publisher replay support
- `publisher.LoggingBetSettlementPublisher` - supported fallback publisher that logs payloads and can replay stored payload snapshots for manual recovery

#### `infrastructure.persistence`

- `adapter.BetQueryPersistenceAdapter` - reads seeded bets from H2 with explicit read-only transaction intent
- `adapter.ProcessedEventOutcomePersistenceAdapter` - event-level dedup tracking backed by one insert attempt plus duplicate handling on unique-key conflicts, timestamped through the shared persistence clock
- `adapter.SettlementAuditPersistenceAdapter` - batched pending audit inserts, direct sent/failed audit status updates, and retryable audit queries for the manual recovery path backed by a composite replay index on publish status, creation time, and bet id, with bounded payload snapshots aligned to the schema contract
- `adapter.DemoStateResetPersistenceAdapter` - clears transient tables for demos/tests
- `entity.*` - JPA mappings for `bet`, `processed_event_outcome`, and `settlement_audit`
- `repository.*` - Spring Data repositories backing the adapters

#### `infrastructure.config`

- `MessagingConfiguration` - chooses the intentional logging publisher by default and requires RocketMQ transport wiring for explicit RocketMQ mode
- `MessagingConfiguration` also provides the shared JSON mapper, the snapshot-focused settlement payload factory exposed through `BetSettlementPayloadPort`, and the settlement payload codec used by publisher implementations
- `PersistenceConfiguration` - exposes the shared UTC clock used by persistence write adapters
- `DomainConfiguration` - exposes domain services such as `BetSettlementDecider` as Spring-managed collaborators without adding Spring to the domain layer
- `BetSettlerMessagingProperties` - typed configuration for topic names, Kafka publish acknowledgement timeout, and publisher mode, including the explicit RocketMQ fail-fast contract
- `DemoProfiles` - centralized local/test profile constants used by demo-only features
- `TransactionConfiguration` - exposes `TransactionOperations` for application orchestration
- `OpenApiConfiguration` - Swagger/OpenAPI metadata for local profiles
- `H2TcpServerConfiguration` / `H2TcpServerProperties` - optional TCP exposure of in-memory H2 in `local-docker`

#### `infrastructure.observability`

- `BetSettlerInfoContributor` - adds service-specific data to `/actuator/info`, including the Kafka publish acknowledgement timeout
- `BetSettlerInfoContributor` also exposes the centralized demo-reset profile list used by local/test helper endpoints
- `MicrometerSettlementFlowMetrics` - records counters and summaries for the settlement flow

## Runtime entry points and hot-path adapters

### Main runtime entry point

- `BetSettlerApplication` - Spring Boot bootstrap class

### HTTP entry points

- `POST /api/v1/event-outcomes` -> `EventOutcomeController` -> `PublishEventOutcomeService`
- `GET /api/v1/bets` -> `BetQueryController` -> `GetBetsQueryService`
- `POST /api/v1/internal/demo/reset` -> `DemoController` -> `ResetDemoStateService`
- `POST /api/v1/internal/demo/retry-settlements` -> `DemoController` -> `RetrySettlementDispatchService`

### Async processing path

- `PublishEventOutcomeService` → `KafkaEventOutcomePublisher` with a short broker-acknowledgement wait
- `KafkaEventOutcomeConsumer` → `ProcessEventOutcomeService`
- `ProcessEventOutcomeService` → `SettlementDispatchPreparationService` for the transactional preparation stage
- `SettlementDispatchPreparationService` → persistence ports, `BetSettlementDecider`, and `BetSettlementPayloadPort`
- `ProcessEventOutcomeService` → `BetSettlementDispatchPort` and settlement audit port for out-of-transaction dispatch updates
- `RetrySettlementDispatchService` → settlement audit port and `BetSettlementReplayPort` for manual replay of `PENDING` / `FAILED` audits
- selected settlement publisher → RocketMQ or logging fallback

## Resource layout

### Configuration files

- `src/main/resources/application.yaml` - common defaults, H2, Kafka, disabled Springdoc by default
- `src/main/resources/application-local-fallback.yaml` - host-run app + Kafka + logging settlement publisher + H2 TCP access
- `src/main/resources/application-local-docker.yaml` - Docker runtime + RocketMQ + optional H2 TCP server
- `src/main/resources/application-test.yaml` - test profile configuration used by automated tests

### IntelliJ run configurations

- `.run/BetSettler [local-fallback].run.xml` - preferred host-run review flow with Kafka plus logging-based settlement dispatch
- `.run/BetSettler [local-docker].run.xml` - host-run app wired to the Docker-backed Kafka and RocketMQ stack
- `.run/BetSettler [test].run.xml` - Spring Boot launch aligned with the automated-test profile family
- `.run/Docker Compose All.run.xml` - starts the full Docker demo stack in the background
- `.run/Docker Compose Queues.run.xml` - starts only Kafka/RocketMQ infrastructure for host-run app scenarios
- `.run/Produce Test Stats.run.xml` - refreshes `utils/test-stats/stats.txt` after a passing build

### Database migrations

- `db/migration/V1__init_schema.sql` - base schema for bets, processed outcomes, and settlement audit
- `db/migration/V2__seed_demo_data.sql` - small deterministic demo dataset for local runs and tests

## Test layout

`src/test/java` mirrors the main package structure and uses explicit test-only support packages where that improves ownership clarity.

- `bootstrap/*` - application context smoke coverage and main-entry bootstrap verification, named explicitly as smoke suites (`BetSettlerApplicationContextSmokeTests`, `BetSettlerApplicationMainSmokeTests`)
- `testsupport/*` - shared test-only infrastructure such as the Kafka Testcontainers configuration and REST integration support harnesses
- `domain/*` - unit tests for records and settlement logic
- `application/*` - orchestration and edge-case tests for use cases, including dedicated `application.model.dispatch` and `application.model.audit` contract coverage for moved application model records
- `infrastructure/config/*` - configuration properties and publisher-selection/profile tests
- `infrastructure/messaging/kafka/integration/*` - publish-side Kafka integration and end-to-end Kafka settlement-flow verification
- `infrastructure/persistence/adapter/*` - adapter-focused integration tests for bet-query, processed-outcome, and settlement-audit concerns, including replay eligibility checks for `PENDING` / `FAILED` settlement audits
- `infrastructure/persistence/schema/*` - schema and index/constraint verification for the Flyway-managed database contract
- `infrastructure/rest/controller/*` - controller-focused REST integration tests for event outcomes, bet queries, and demo helper endpoints
- `infrastructure/rest/actuator/*` - actuator integration coverage
- `infrastructure/rest/openapi/*` - OpenAPI and Swagger UI integration coverage
- `infrastructure/rest/profile/*` - profile-gated REST behavior tests
- `testsupport/rest/*` - shared REST test support split into a narrow HTTP helper (`HttpRestIntegrationTestSupport`), an opt-in configurable publisher override (`ConfigurableEventOutcomePublisherTestConfiguration`), and a focused seeded settlement-audit fixture support (`SettlementAuditRestIntegrationTestSupport`)
- `infrastructure/*` subpackages - focused unit tests for adapters, entities, mappers, observability components, and message contracts

## Known structure and cohesion candidates

- `ProcessEventOutcomeService` is slimmer than before but still owns publication-loop metrics and failure handling, so it remains the main orchestration hotspot.
- REST integration tests mirror their concerns more closely, so future cleanup should focus on behavior changes rather than recovering package intent.

## Related documents

- See [`README.md`](README.md) for the practical entry guide.
- See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the end-to-end flow and diagrams.
- See [`QUALITY.md`](QUALITY.md) for build gates, CI workflows, and current verification stats.
