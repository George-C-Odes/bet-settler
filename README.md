[![Java Coverage](https://github.com/George-C-Odes/bet-settler/actions/workflows/java_coverage.yml/badge.svg)](https://github.com/George-C-Odes/bet-settler/actions/workflows/java_coverage.yml)
[![Qodana](https://github.com/George-C-Odes/bet-settler/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/George-C-Odes/bet-settler/actions/workflows/qodana_code_quality.yml)

# bet-settler

`bet-settler` is a Spring Boot backend service that simulates sports betting event-outcome handling and bet-settlement dispatch.

It covers the assignment baseline:

- `POST /api/v1/event-outcomes` accepts an outcome and publishes it to Kafka
- a Kafka consumer listens on `event-outcomes`
- matching bets are loaded from an H2 in-memory database by `eventId`
- settlement decisions are produced as `WIN` or `LOSE`
- settlement messages are sent to RocketMQ topic `bet-settlements`
- a supported logging fallback mode exists when RocketMQ is not available

## Documentation map

Start here for practical usage, then follow the focused docs as needed:

- [`STRUCTURE.md`](STRUCTURE.md) - package layout, layer ownership, and key adapters
- [`ARCHITECTURE.md`](ARCHITECTURE.md) - end-to-end flow, transactional model, and Mermaid diagrams
- [`QUALITY.md`](QUALITY.md) - Maven quality gates, GitHub Actions workflows, and current test/coverage stats
- [`HELP.md`](HELP.md) - minimal supplementary Spring Boot reference links

The support docs are intended to stay aligned with the latest passing verification snapshot rather than act as one-off design notes.

## What the service does

High-level flow:

1. the API accepts an event outcome
2. the outcome is published to Kafka topic `event-outcomes` and the API waits briefly for broker acknowledgement before returning `202`
3. a Kafka consumer triggers the processing use case
4. the service deduplicates by atomically recording `eventId` in `processed_event_outcome`
5. matching bets are loaded from H2
6. settlement audit rows are written as `PENDING` during the transactional preparation step
7. those audit rows keep a bounded `payload_snapshot` (`VARCHAR(4096)`) plus lightweight DB checks for valid settlement status values, so the schema still stays small while guarding the hot path against drift
8. settlements are dispatched to RocketMQ or logged in fallback mode, while duplicate outcomes short-circuit through an explicit duplicate result instead of using a fake destination marker
9. audit rows are updated to `SENT` or `FAILED` after each publication attempt using shared UTC timestamping from the persistence configuration
10. local/test operators can replay `PENDING` or `FAILED` audit rows through a demo retry endpoint

This service is intentionally the trigger-and-dispatch part of the flow, not a full financial settlement engine.

Internally, the processing path keeps high-level orchestration in `ProcessEventOutcomeService`, transactional preparation in a focused collaborator, neutral application-model records for dispatch planning and settlement-audit descriptors, destination-topic and payload-snapshot creation behind a narrower settlement payload port, a dedicated settlement payload codec for shared message mapping plus stored-snapshot replay restoration, failure-reason formatting in a dedicated helper, the winner/loser rule in a separate domain service, and a single insert-first processed-outcome persistence path for deduplication.

The persistence layer remains intentionally lightweight: query-only bet reads declare read-only transaction intent, write adapters share a single UTC `Clock` bean for deterministic timestamp ownership, and the base Flyway schema aligns the audit payload column with the existing bounded entity mapping instead of using a heavier LOB type.

On the HTTP side, the REST adapter uses controller-focused mappers for event-outcome publication, bet-query responses, and demo-helper responses instead of routing all three concerns through one umbrella mapper.

Shared settlement transport DTOs, mapping, snapshot creation, replay codec support, and the logging fallback publisher live under the neutral `infrastructure.messaging.settlement` package, while the real `RocketMqBetSettlementPublisher` remains in the RocketMQ-specific package.

The test tree mirrors those ownership boundaries more closely as well: bootstrap smoke tests use smoke-oriented names under `bootstrap`, shared Kafka Testcontainers wiring lives under `testsupport`, Kafka integration flows live under `infrastructure.messaging.kafka.integration`, the larger REST integration coverage is split by controller/actuator/OpenAPI concern, the shared REST harness is intentionally narrow with a reusable HTTP helper plus smaller opt-in fixtures for configurable event-outcome publication and seeded settlement-audit setup, persistence integration coverage is split by adapter responsibility instead of staying in umbrella test classes, `PreparedDispatchPlan` has dedicated contract coverage beside its production package, and the manual retry path is explicitly frozen with tests around `PENDING`/`FAILED` eligibility plus stored destination-topic and payload-snapshot replay semantics.

## Stack summary

- Java 25
- Spring Boot 4.0.6
- Spring Web MVC + Bean Validation
- Spring Data JPA + Flyway + H2
- Spring for Apache Kafka
- RocketMQ Spring integration
- Spring Boot Actuator + Micrometer
- Testcontainers for Kafka integration tests
- Spotless, PMD, CPD, SpotBugs, FindSecBugs, JaCoCo, Javadoc checks

## Runtime profiles

| Profile          | Typical usage                  | Settlement publisher      |
|------------------|--------------------------------|---------------------------|
| `local-fallback` | app on host, Kafka from Docker | logging fallback          |
| `local-docker`   | full Docker-backed stack       | real RocketMQ             |
| `test`           | automated tests                | test-specific wiring      |
| default          | base config only               | logging unless overridden |

When `betsettler.messaging.rocketmq.publisher-mode=rocketmq` is configured explicitly, startup fails fast unless a `RocketMQTemplate` is actually available. The supported downgrade path is to select `logging` mode intentionally, not to request RocketMQ and silently fall back.

## IntelliJ run configurations in `.run/`

The repository includes six ready-to-use IntelliJ run configurations, so reviewers can switch between the most common local flows without recreating commands by hand.

| Run configuration             | What it starts                                                              | When to use it                                                                                                                                                                                                                                             |
|-------------------------------|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BetSettler [local-fallback]` | Spring Boot app with the `local-fallback` profile                           | Fastest host-run review path: Kafka stays external, settlement dispatch uses the supported logging fallback, and Swagger/OpenAPI are enabled                                                                                                               |
| `BetSettler [local-docker]`   | Spring Boot app with the `local-docker` profile                             | Use when Kafka and RocketMQ are available from the Docker stack and you want to exercise the real RocketMQ publisher. If the app runs outside Docker, switch the `rocketmq-broker` command in `compose.yaml` to the commented `broker.conf` variant first. |
| `BetSettler [test]`           | Spring Boot app with the `test` profile                                     | Handy for inspecting the same profile family used by automated tests, including test-oriented topic names and local helper endpoints                                                                                                                       |
| `Docker Compose All`          | `docker compose --profile=queues --profile=app up -d`                       | Starts the full local demo stack in the background: Kafka, Kafka UI, RocketMQ services, dashboards, and the app container                                                                                                                                  |
| `Docker Compose Queues`       | `docker compose --profile=queues up -d`                                     | Starts only the queue infrastructure when you want to run the app on the host, especially together with `BetSettler [local-fallback]`                                                                                                                      |
| `Produce Test Stats`          | `powershell -ExecutionPolicy Bypass -File .\utils\test-stats\extractor.ps1` | Refreshes the committed `utils/test-stats/stats.txt` snapshot after a passing verification build                                                                                                                                                           |

All three shell-based configurations are configured to run from the project root through PowerShell, matching the checked-in Windows-oriented command examples in this repository.

## Prerequisites

- Java 25
- Docker Desktop or another Docker engine with Compose support for the provided local flows

The Maven wrapper is included, so a separate Maven installation is not required.

## Quick start

### Fastest review path: host app + Kafka + logging fallback

Start Kafka and Kafka UI:

```bat
docker compose -f compose.yaml up -d kafka kafka-ui
```

Run the app with the fallback profile:

```bat
.\mvnw.cmd -Dspring-boot.run.profiles=local-fallback spring-boot:run
```

What you get in this mode:

- real Kafka
- Flyway-seeded in-memory H2 data
- H2 TCP access on `localhost:9093` for host-side database clients
- settlement payloads logged instead of sent to RocketMQ
- Swagger/OpenAPI enabled locally

This is also the recommended host-run profile when you do **not** have RocketMQ transport wiring available, because explicit RocketMQ mode fails to start up instead of silently degrading.

Useful URLs:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Kafka UI: `http://localhost:8081`
- H2 TCP: `localhost:9093`

H2 host-side connection details in `local-fallback` and `local-docker`:

- JDBC URL: `jdbc:h2:tcp://localhost:9093/mem:betsettler`
- Username: `sa`
- Password: blank
- Driver: `org.h2.Driver`

### Full Docker-backed stack

```bat
docker compose -f compose.yaml --profile queues --profile app up --build
```

What you get in this mode:

- app running in Docker with the `local-docker` profile
- Kafka, Kafka UI, RocketMQ name server, RocketMQ broker, and RocketMQ dashboard
- real settlement publication to RocketMQ
- optional host access to the in-memory H2 database over TCP

This profile intentionally requests RocketMQ mode, so the application fails fast during startup if the RocketMQ transport bean cannot be created.

> Important: the default `rocketmq-broker` command in `compose.yaml` is intentionally Docker-first so the `bet-settler` app container can publish through RocketMQ. If you want to run the app outside Docker with the `local-docker` profile (for example from IntelliJ), comment out that default broker command and switch to the commented `broker.conf`-based broker command and volume mount in `compose.yaml`, then recreate the broker container.

Useful URLs:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Kafka UI: `http://localhost:8081`
- RocketMQ dashboard: `http://localhost:8082`
- H2 TCP: `localhost:9093`

H2 host-side connection details in `local-fallback` and `local-docker`:

- JDBC URL: `jdbc:h2:tcp://localhost:9093/mem:betsettler`
- Username: `sa`
- Password: blank
- Driver: `org.h2.Driver`

Stop the local infrastructure:

```bat
docker compose -f compose.yaml --profile queues --profile app down
```

## API summary

### `POST /api/v1/event-outcomes`

Publishes an event outcome for asynchronous processing.

Example request body:

```json
{
  "eventId": "EVT-1001",
  "eventName": "Team A vs Team B",
  "eventWinnerId": "TEAM-A"
}
```

Behavior:

- waits briefly for Kafka broker acknowledgement before returning `202 Accepted`
- uses `eventId` as the Kafka message key
- returns structured validation errors for invalid requests
- returns `503 Service Unavailable` if Kafka does not acknowledge the publish within the configured timeout

Example:

```bat
curl.exe -i -X POST http://localhost:8080/api/v1/event-outcomes -H "Content-Type: application/json" -d "{\"eventId\":\"EVT-1001\",\"eventName\":\"Team A vs Team B\",\"eventWinnerId\":\"TEAM-A\"}"
```

### `GET /api/v1/bets`

Returns all seeded bets.

```bat
curl.exe http://localhost:8080/api/v1/bets
```

### `GET /api/v1/bets?eventId=EVT-1001`

Returns only bets for the supplied event.

```bat
curl.exe "http://localhost:8080/api/v1/bets?eventId=EVT-1001"
```

### `POST /api/v1/internal/demo/reset`

Available only in `local-docker`, `local-fallback`, and `test` profiles.
It clears the transient processing state while preserving Flyway-seeded bets.

```bat
curl.exe -X POST http://localhost:8080/api/v1/internal/demo/reset
```

### `POST /api/v1/internal/demo/retry-settlements`

Available only in `local-docker`, `local-fallback`, and `test` profiles.
It replays settlement audit rows that are still `PENDING` or previously marked `FAILED` by resending each row's stored `destinationTopic` and stored `payloadSnapshot` through the currently configured settlement transport.

```bat
curl.exe -X POST http://localhost:8080/api/v1/internal/demo/retry-settlements
```

## Demo seed data

Flyway seeds four bets for local verification:

| Event ID   | Bet ID     | User ID  | Predicted winner | Amount |
|------------|------------|----------|------------------|-------:|
| `EVT-1001` | `BET-1001` | `USER-1` | `TEAM-A`         |  25.00 |
| `EVT-1001` | `BET-1002` | `USER-2` | `TEAM-B`         |  10.00 |
| `EVT-2001` | `BET-2001` | `USER-3` | `TEAM-C`         |   5.50 |
| `EVT-3001` | `BET-3001` | `USER-4` | `TEAM-D`         |  18.75 |

Useful extra verification event:

- `EVT-9999` has no seeded bets and exercises the no-match path

## Configuration highlights

Important properties:

- `spring.kafka.bootstrap-servers`
- `betsettler.messaging.kafka.publish-ack-timeout`
- `betsettler.messaging.kafka.event-outcomes-topic`
- `betsettler.messaging.rocketmq.bet-settlements-topic`
- `betsettler.messaging.rocketmq.publisher-mode` (`logging` for intentional fallback, `rocketmq` for required RocketMQ transport)
- `rocketmq.name-server`
- `rocketmq.producer.group`

Default logical destinations:

- Kafka topic: `event-outcomes`
- RocketMQ topic: `bet-settlements`

Common environment variable overrides:

- `KAFKA_BOOTSTRAP_SERVERS`
- `BET_SETTLER_KAFKA_PUBLISH_ACK_TIMEOUT`
- `BET_SETTLER_EVENT_OUTCOMES_TOPIC`
- `BET_SETTLER_BET_SETTLEMENTS_TOPIC`
- `BET_SETTLER_ROCKETMQ_PUBLISHER_MODE`
- `ROCKETMQ_NAME_SERVER`
- `ROCKETMQ_PRODUCER_GROUP`

Persistence contract highlights:

- `bet.bet_amount` is protected by a lightweight positive-value check in the base schema
- `settlement_audit.payload_snapshot` is stored as bounded `VARCHAR(4096)` to match the entity mapping and current compact JSON snapshots
- `settlement_audit.settlement_result` is constrained to `WIN` / `LOSE`
- `settlement_audit.publish_status` is constrained to `PENDING` / `SENT` / `FAILED`

Publisher-mode contract:

- `logging` is the intentional fallback/default mode and always selects the logging settlement publisher
- `rocketmq` requires RocketMQ auto-configuration to contribute a `RocketMQTemplate`
- if `rocketmq` is requested without that transport bean, the application fails startup by design

## Observability

Actuator endpoints are exposed by default:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

Custom Micrometer meters include:

- `betsettler.event.outcomes.accepted`
- `betsettler.event.outcomes.processed`
- `betsettler.settlements.prepared`
- `betsettler.settlements.dispatched`
- `betsettler.demo.reset.invocations`
- `betsettler.demo.reset.rows.cleared`

## Build and verification

```bat
.\mvnw.cmd spotless:check
.\mvnw.cmd verify
.\mvnw.cmd -q -B test-compile
.\mvnw.cmd clean verify
```

Use `clean verify` as the final local handoff check. In CI, the Java coverage workflow runs `verify`, while the Qodana workflow performs a `test-compile` preflight before scanning.

The current committed verification snapshot is stored in `utils/test-stats/stats.txt` and summarized in [`QUALITY.md`](QUALITY.md), which should be refreshed only after a passing `clean verify`.

To refresh the committed stats snapshot after a passing build:

```powershell
powershell -ExecutionPolicy Bypass -File .\utils\test-stats\extractor.ps1
```

See [`QUALITY.md`](QUALITY.md) for the detailed build gates, workflow triggers, artifact outputs, and the current coverage/test snapshot.

## Future improvements, production readiness, and scaling directions

The current implementation intentionally stops at a pragmatic assignment-ready design. The items below are **future directions**, not part of the current delivered behavior.

- strengthen idempotency beyond the current `eventId`-only dedup key, especially if upstream producers can resend semantically different payloads with the same business identity
- replace the current transaction-plus-audit pattern with a fuller outbox-style reliability design if stronger delivery guarantees become a production requirement
- formalize outbound timeout, cancellation, and retry policies rather than relying on the current manual replay path for recovery
- add automated retry-with-backoff orchestration and a DLQ strategy only when operational requirements justify the extra moving parts
- review Kafka partitioning, consumer concurrency, and RocketMQ throughput settings for higher event volumes or wider tenant/event fan-out
- introduce stronger operational alerting around repeated publish failures, replay backlogs, and unusual no-match/duplicate-event rates
- harden persistence for production workloads by moving from in-memory H2 to a durable database and by reviewing retention/archival rules for settlement-audit data
- expand runtime hardening with stricter secret management, environment-specific access controls, and richer deployment health/readiness checks

## Known limitations

- deduplication is basic and keyed by `eventId`
- DB writes and outbound publication are not a full transactional outbox
- failed outbound dispatch can be retried manually in local/test profiles, but there is still no automatic replay worker
- H2 is in-memory and resets on application restart

## Troubleshooting

### Kafka connection fails in `local-fallback`

Start Kafka first:

```bat
docker compose -f compose.yaml up -d kafka kafka-ui
```

The fallback profile expects Kafka on `localhost:29092` unless overridden.

### The reset endpoint returns 404

Use one of these profiles:

- `local-docker`
- `local-fallback`
- `test`

### Posting the same event twice only processes once

That is expected. The current dedup logic records processed outcomes by `eventId` with one insert attempt into `processed_event_outcome`, and any unique-key conflict is treated as a duplicate.

### The app fails to start after setting `BET_SETTLER_ROCKETMQ_PUBLISHER_MODE=rocketmq`

That means the runtime did exactly what the configuration asked for: explicit RocketMQ mode requires a real `RocketMQTemplate`.

Use one of these fixes:

- run the full Docker-backed flow so RocketMQ autoconfiguration can create the transport bean
- switch back to `local-fallback`
- set `BET_SETTLER_ROCKETMQ_PUBLISHER_MODE=logging` when you want the supported logging fallback instead
