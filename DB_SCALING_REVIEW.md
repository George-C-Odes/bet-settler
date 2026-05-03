# Database review: high load and horizontal-scaling readiness

## Scope

This review focuses on the current database setup and every main read/write interaction in the codebase, based on the implementation in:

- `src/main/resources/application*.yaml`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__seed_demo_data.sql`
- `src/main/java/io/github/georgecodes/betsettler/infrastructure/persistence/**`
- `src/main/java/io/github/georgecodes/betsettler/application/service/**`
- `src/main/java/io/github/georgecodes/betsettler/infrastructure/messaging/kafka/consumer/KafkaEventOutcomeConsumer.java`
- `src/test/java/io/github/georgecodes/betsettler/infrastructure/persistence/**`

The goal here is not to judge the assignment solution against the exercise constraints, but to assess how the current persistence design would behave under **high event throughput**, **large fan-out per event**, and **multiple application instances**.

---

## Executive summary

The current persistence implementation is **clean, explicit, and well-structured for an assignment/demo service**, but it is **not ready for heavy load or true horizontal scaling** in its present form.

The main reasons are:

1. **The database is process-local in-memory H2**.
   - Each application instance gets its own isolated state.
   - Deduplication, bet visibility, audit history, and retry state are therefore not shared across nodes.
   - This is the single biggest blocker for horizontal scaling.

2. **The main settlement flow performs per-event read/write work in one growing transaction, then per-settlement updates outside the transaction**.
   - For events with many matching bets, transaction time, memory usage, and connection occupancy all scale linearly.
   - After preparation, each settlement generates its own status update writing, which becomes expensive at volume.

3. **The retry flow is not work-claim-based**.
   - It reads all retryable rows at once.
   - It does not lease or claim rows for one worker/node.
   - In a scaled deployment, multiple nodes could replay the same rows.

4. **The schema and query model are still “small-data” oriented**.
   - There is no partitioning, retention, archival, or bounded replay window.
   - Queries are unpaged and entity-based.
   - Pooling and batching are left using defaults.

That said, the implementation already has some good foundations:

- insert-first deduplication through a database key on `processed_event_outcome.event_id`
- a useful composite index on `bet(event_id, bet_id)`
- a useful composite replay index on `settlement_audit(publish_status, created_at, bet_id)`
- read-only transaction intent on bet reads
- direct update statements for audit status changes instead of select-then-save
- database constraints protecting basic data integrity

So the code is **directionally solid**, but the runtime database choice and hot-path interaction model need to change for scale.

---

## Current database setup

### What exists today

From `application.yaml` and `application-test.yaml`:

- datasource: `jdbc:h2:mem:...`
- Flyway-managed schema
- JPA/Hibernate with `ddl-auto: none`
- no explicit connection-pool tuning
- no explicit Hibernate batching configuration
- no explicit transaction timeout configuration

From `H2TcpServerConfiguration`:

- H2 TCP exposure is enabled for local profiles, but this **does not turn the app into a shared durable database setup**.
- It only exposes the same in-memory database of that one process for local inspection.

### Why this is fine for the assignment

It matches the task requirement to use an in-memory database for bets and keeps the solution easy to run.

### Why this is a hard blocker for scale

For high load and horizontal scaling, the current setup has these structural limits:

- **No shared state across instances**
  - node A and node B will each have different `processed_event_outcome` rows
  - dedup becomes node-local, not service-wide
  - retryable audit rows become node-local, not operationally manageable
  - Flyway seed data is duplicated separately in each node
- **No durability**
  - restarts wipe dedup and audit history
  - replay state disappears
- **No realistic lock/contention behavior** compared with a production RDBMS
- **No separation between demo runtime and scalable runtime**
  - there is no external database profile to validate future production behavior

### Recommendation

Introduce a second persistence mode for scale testing and production-style operation:

- keep H2 for assignment/demo convenience
- add a real shared database profile, preferably **PostgreSQL**
- run Flyway against that shared database
- test multi-instance behavior against the shared DB, not H2

This should be treated as the first required step before most other scaling changes.

---

## Schema review

## `bet`

Current shape:

- primary key: `bet_id`
- lookup index: `idx_bet_event_id_bet_id` on `(event_id, bet_id)`
- positive amount check

### What is good

The composite `(event_id, bet_id)` index aligns well with the hot query:

- `findByEventIdOrderByBetIdAsc(...)`

That is the right index for the current main settlement lookup.

### Scale concerns

- `GET /api/v1/bets` uses `findAllByOrderByBetIdAsc()` and is fully unbounded.
- For huge bet volumes, full-ordered scans will become expensive.
- If some events accumulate huge numbers of bets, reading the entire event slice in one shot can create large memory pressure and long transactions.

### Recommendation

- Keep the `(event_id, bet_id)` index.
- Add pagination/keyset access patterns for operational reads.
- If event fan-out can be huge, process bets in chunks such as:
  - `where event_id = ? and bet_id > ? order by bet_id limit ?`

---

## `processed_event_outcome`

Current shape:

- primary key: `event_id`
- stores `event_name`, `event_winner_id`, `processed_at`

### What is good

The current insert-first dedup approach is efficient for a single shared SQL database:

- one insert attempt
- unique-key conflict means duplicate
- no read-before-write race window

That is a good pattern.

### Scale concerns

- With the current H2 setup, dedup is only valid per process.
- The table will grow monotonically if moved to a durable database and kept forever.
- There is no retention/TTL strategy.
- There is no secondary index to support cleanup by `processed_at`.

### Recommendation

If this table remains the dedup store in a shared database:

- keep the primary-key dedup model
- add a retention policy based on the business replay horizon
- add cleanup support, likely including an index on `processed_at`
- define whether dedup is forever, time-bounded, or event-version aware

For example:

- if upstream can resend the same event for 7 days, keep 7–30 days of dedup state
- if upstream can legitimately send event corrections, dedup by a stronger business key than plain `event_id`

---

## `settlement_audit`

Current shape:

- surrogate key: `audit_id BIGINT GENERATED BY DEFAULT AS IDENTITY`
- unique constraint: `(event_id, bet_id)`
- retry index: `(publish_status, created_at, bet_id)`
- payload snapshot stored inline as `VARCHAR(4096)`

### What is good

- The unique `(event_id, bet_id)` constraint protects against duplicate audit creation for one event/bet pair.
- The replay index is better than a status-only index.
- The schema-level status constraints are useful integrity guards.

### Scale concerns

1. **Insert batching is limited by the current JPA strategy**
   - `saveAll(...)` is used for pending rows.
   - `audit_id` uses `IDENTITY`.
   - In practice, `IDENTITY` usually reduces or disables efficient Hibernate insert batching because generated keys must be retrieved row by row.

2. **There is no lifecycle management**
   - if this becomes the operational audit trail in a real database, it will grow indefinitely
   - replay queries, retention jobs, and indexes will all get more expensive over time

3. **The table mixes hot operational state and historical audit storage**
   - `PENDING` / `FAILED` rows are hot operational data
   - `SENT` rows quickly become cold history
   - keeping both concerns in one ever-growing table can hurt hot-query performance

4. **The current retry index is not enough for a real retry worker**
   - there is no `next_attempt_at`
   - there is no `attempt_count`
   - there is no lease/claim state
   - there is no worker ownership or timeout

### Recommendation

Short term:

- keep the unique `(event_id, bet_id)` key
- consider replacing `IDENTITY` with:
  - a database sequence with allocation/buffering, or
  - application-generated IDs (UUIDv7 / ULID), if a surrogate key is still needed
- add operational retry columns if this table remains the retry source:
  - `attempt_count`
  - `next_attempt_at`
  - `lease_until`
  - `leased_by`
  - optionally `last_error_code` / `last_error_class`

Medium term:

- partition or archive `SENT` history by time
- keep the hot retry set small
- consider splitting:
  - an **outbox / dispatch queue table** for hot operational work
  - a separate **audit/history table** for long-term observability

---

## Read/write interaction review

## 1. Event processing hot path

Main flow:

- `KafkaEventOutcomeConsumer.consume(...)`
- `ProcessEventOutcomeService.process(...)`
- transactional `SettlementDispatchPreparationService.prepare(...)`

### Current database interactions per consumed event

1. insert into `processed_event_outcome` via `recordProcessedIfAbsent(...)`
2. read matching bets via `findByEventIdOrderByBetIdAsc(...)`
3. insert pending audit rows via `savePendingSettlements(...)`
4. after the transaction, for each settlement:
   - update `settlement_audit` to `SENT`, or
   - update `settlement_audit` to `FAILED`

### What is good

- dedup is attempted before the heavier work
- the transaction covers the preparation state consistently
- remote publication is correctly outside the DB transaction
- status updates are direct SQL updates instead of select-modify-save

### Scale concerns

#### A. Transaction size grows with event fan-out

`SettlementDispatchPreparationService.prepare(...)` reads **all bets for one event** into memory, maps them all, builds payload snapshots for all, and writes all audit rows in one transaction.

For events with many bets, this means:

- longer transaction duration
- more heap usage
- longer connection occupancy
- larger flush work at commit time
- more lock/index pressure on the audit table

This is likely the biggest hot-path bottleneck after the H2 choice itself.

#### B. Row-by-row status updates after dispatch

After preparation, `ProcessEventOutcomeService.publishPreparedDispatchPlan(...)` does one publish and then one SQL update per settlement.

That produces a write pattern like:

- 1 dedup insert
- 1 bet read
- N pending-audit inserts
- N status updates

For a high-fan-out event, the total DB round trips become large.

#### C. No backpressure boundary between database preparation and dispatch volume

If one event matches thousands of bets, the code prepares all rows first and then iterates them all. There is no chunking, dispatch window, or database-driven claim process.

#### D. No explicit transaction timeout

Large events could keep a transaction open for too long under contention or slow I/O around commit pressure.

### Recommendation

For a higher scale, change the event flow from “whole event in one unit” to “event in chunks”:

1. Record the event as accepted/deduplicated.
2. Fetch matching bets in pages.
3. Insert dispatch work in pages.
4. Let a dispatcher/outbox worker publish claimed work separately.

Concretely:

- replace `findByEventIdOrderByBetIdAsc(...)` with chunked/keyset reads
- batch-write outbox/dispatch rows in the same transaction as dedup preparation
- move `SENT` / `FAILED` updates to worker-side batched transitions
- set transaction timeouts explicitly

This is the path that scales both vertically and horizontally.

---

## 2. Bet reads

Current read methods:

- `getAllBets()` -> `findAllByOrderByBetIdAsc()`
- `getBetsByEventId(eventId)` -> `findByEventIdOrderByBetIdAsc(eventId)`

### What is good

- read-only transaction intent is explicit
- the main event lookup query matches the existing index

### Scale concerns

#### A. Full-table read endpoint is unbounded

`findAllByOrderByBetIdAsc()` is acceptable for tiny demo seed data, but it is not safe as a production-style API once bet volume grows.

#### B. Full event slice is unbounded

Even indexed reads can be too large if one event has very high bet fan-out.

#### C. Entity materialization is eager and whole-result based

The adapter loads entities into a list and then maps the whole list into domain records.

### Recommendation

- add pagination to all user-facing read endpoints
- for internal hot-path processing, use chunked reads instead of list-all reads
- consider projection-based queries when not every column is needed
- reserve `findAll...` only for small demo/admin scenarios

---

## 3. Pending-audit writes

Current method:

- `SettlementAuditPersistenceAdapter.savePendingSettlements(List<PendingSettlementAudit>)`
- implemented via `settlementAuditRepository.saveAll(...)`

### What is good

- the code avoids per-row timestamp generation drift by sharing one `createdAt`
- the method accepts a batch list rather than being called once per row

### Scale concerns

#### A. `saveAll(...)` is not enough by itself to guarantee real batching

Because there is no explicit Hibernate batching configuration and the entity uses `IDENTITY`, the real runtime behavior is likely still row-oriented.

#### B. Payload snapshots are generated and persisted inline for every row

That is fine for modest volume, but it increases write size and memory pressure for large settlement batches.

### Recommendation

If keeping the current audit-table approach:

- configure real JDBC batching explicitly
- avoid `IDENTITY` if you want Hibernate batching to help
- consider a lower-level bulk insert path for the hot write path if profiling shows JPA overhead
- measure payload size distribution and keep the row as compact as possible

If moving toward a stronger scale design:

- write a purpose-built outbox/dispatch table with only the fields needed for dispatch
- move long-form audit/history details to a colder path

---

## 4. Status updates after publish

Current methods:

- `markSent(eventId, betId)`
- `markFailed(eventId, betId, failureReason)`
- both execute direct update statements

### What is good

- avoids select-before-update
- it validates that exactly one row was updated

### Scale concerns

#### A. One update per settlement

This becomes expensive with large numbers of settlements.

#### B. Lookup key is business-composite, not the generated audit key

The unique `(event_id, bet_id)` index likely makes this acceptable, but if the dispatcher already knew `audit_id`, updates could be cheaper and simpler.

#### C. No compare-and-swap state transition

The update statement does not check the previous state. In a multi-worker future, a stronger update shape is safer, for example:

```sql
update settlement_audit
   set publish_status = 'SENT', published_at = ?, failure_reason = null
 where audit_id = ?
   and publish_status = 'IN_PROGRESS'
```

That pattern prevents stale workers from overwriting the state unexpectedly.

### Recommendation

- in the current model, add bulk update options where practical
- in a scaled worker model, transition rows with compare-and-swap semantics
- prefer worker-owned row identifiers or leases over repeated business-key updates

---

## 5. Retry flow

Current flow:

- `findRetryableSettlements()` returns **all** `PENDING` and `FAILED` rows ordered by `created_at`, `bet_id`
- `RetrySettlementDispatchService` iterates them all
- each row is replayed and then updated individually

### What is good

- the read query matches the replay index reasonably well
- the design is straightforward and explicit

### Scale concerns

This is one of the clearest areas that would break under a multi-node or high-volume operation.

#### A. Reads all retryable rows at once

That is unbounded in both memory and run time.

#### B. No claim/lease mechanism

Two nodes could replay the same rows.

#### C. `PENDING` rows are retryable immediately

That is risky once normal dispatch and replay can overlap.

#### D. No retry scheduling

There is no backoff, no `next_attempt_at`, no attempt counter, and no dead-letter threshold.

### Recommendation

Replace the replay model with a worker-safe claim process:

1. select a limited batch of eligible rows
2. atomically claim them for one worker
3. publish them
4. transition each row based on the result
5. schedule the next retry time on failure

Useful SQL/database patterns here include:

- `FOR UPDATE SKIP LOCKED` on databases that support it
- lease columns (`leased_by`, `lease_until`)
- `next_attempt_at <= now()`
- fixed batch size per poll
- max-attempt cutoff / DLQ state

This is essential for horizontal scaling.

---

## 6. Demo reset path

Current flow:

- `count()` processed outcomes
- `count()` audits
- `deleteAllInBatch()` audits
- `deleteAllInBatch()` processed outcomes
- `count()` bets

### Assessment

This is acceptable for a demo-only endpoint and should not drive production design.

### Note

If this code were ever used in non-demo workloads, repeated table-wide counts and deletes would be expensive and locking-heavy. As implemented today, it is clearly demo-oriented and should remain outside the main scaling discussion.

---

## Connection management and transaction configuration

### Current situation

I did not find explicit tuning for:

- Hikari maximum pool size
- minimum idle connections
- connection timeout
- max lifetime / idle timeout
- statement caching / prepared statement tuning
- transaction timeout
- Hibernate batch size / ordered inserts / ordered updates

### Why this matters

Under load, database capacity is often lost not only to SQL shape, but to default pooling behavior:

- too few connections → consumer/HTTP contention and queueing
- too many connections → database thrash
- no transaction timeout → long-running event batches monopolize connections

### Recommendation

When moving to a shared production-style database, explicitly tune:

- `spring.datasource.hikari.maximum-pool-size`
- `spring.datasource.hikari.minimum-idle`
- `spring.datasource.hikari.connection-timeout`
- `spring.datasource.hikari.max-lifetime`
- `spring.datasource.hikari.idle-timeout`
- transaction timeouts for the preparation step
- Hibernate/JDBC batching properties where still using JPA for hot writes

Also, consider separate read vs. write pools only if profiling shows clear value and the complexity is justified.

---

## Horizontal-scaling assessment

## Can the current design scale out by adding more app instances?

**Not safe in its current database setup.**

### Why not

1. **Each node has its own H2 in-memory database**
   - dedup is per node
   - audit is per node
   - retry is per node
   - seeded bets are duplicated per node

2. **There is no shared claim model for retryable work**
   - duplicate replays become likely once replay is automated or run from multiple nodes

3. **There is no outbox/queue table for database-backed dispatch coordination**
   - the system relies on in-memory iteration immediately after the transaction

4. **There is no partitioning between hot and cold operational data**
   - one ever-growing audit table will become progressively harder to work with

### What would make horizontal scaling realistic

Minimum viable changes:

- move to a shared durable RDBMS
- keep insert-first dedup in that shared DB
- introduce worker-safe retry claiming
- page/chunk large bet sets
- add pool and timeout tuning

Recommended stronger design:

- shared durable RDBMS
- transactional outbox / dispatch-work table
- separate dispatcher workers
- batched claim-and-send loop
- archival/partitioning for history

---

## Prioritized improvement backlog

## P0 – required before serious horizontal scaling

### 1. Replace process-local H2 with a shared durable database profile

Suggested target: PostgreSQL.

Why first:

- without this, dedup and audit consistency stop at one JVM
- almost every other scaling improvement depends on a shared state

### 2. Rework retry into a claim/lease-based worker flow

Why first:

- the current retry logic is not multi-worker safe
- it will duplicate work under horizontal scaling

### 3. Chunk settlement preparation by event fan-out

Why first:

- it prevents one hot event from turning into one huge transaction and one huge in-memory list

---

## P1 - high-value throughput improvements

### 4. Add explicit pooling and transaction timeout tuning

Why:

- default settings are rarely right for a sustained load

### 5. Make write batching real, not just API-level batching

Why:

- `saveAll(...)` alone is not sufficient evidence of efficient batch inserts
- `IDENTITY` is a likely batching limiter

### 6. Introduce retry scheduling columns and backoff semantics

Why:

- needed for predictable failure handling at volume

---

## P2 - medium-term schema and lifecycle improvements

### 7. Add retention/archival strategy

Tables affected:

- `processed_event_outcome`
- `settlement_audit`

Why:

- both will otherwise grow without a bound in a durable setup

### 8. Separate hot dispatch state from cold audit history

Why:

- improves hot-path query performance and reduces operational table bloat

### 9. Add pagination/projections for non-hot-path reads

Why:

- avoids accidental full scans once data grows

---

## P3 – observability and verification improvements

### 10. Add database-focused load and concurrency tests

The current tests are good for correctness and schema integrity, but they do not prove scale behavior.

Add tests for:

- concurrent duplicate processing of the same `eventId`
- large fan-out event preparation
- multi-worker retry claiming
- lock contention and transaction timeout behavior
- batched insert/update effectiveness against the chosen production database

### 11. Add DB operational telemetry

Track at least:

- connection-pool saturation
- transaction duration for preparation
- rows inserted per event
- rows updated per dispatch batch
- retry backlog size
- retry age / oldest pending row
- dead-letter counts

---

## Concrete target design for a scalable version

A practical next-step design could be:

1. **Shared PostgreSQL database**
2. `processed_event_outcome` remains the dedup table
3. Replace direct pending-audit writes on the hot path with a dedicated `settlement_outbox` table
4. In the same transaction:
   - insert dedup row
   - read matching bets in chunks
   - insert outbox rows in chunks
5. Separate dispatcher workers:
   - claim outbox rows in limited batches
   - publish to RocketMQ
   - update state with compare-and-swap semantics
6. Move long-term history into:
   - archived audit table, or
   - partitioned history table
7. Keep the current `settlement_audit` shape only as a history/reporting concern, not as the hot dispatch queue

This preserves the current clean separation of responsibilities while making the persistence model much more scale-friendly.

---

## Final assessment

The current implementation is **well-designed for the assignment’s runtime simplicity**, and several persistence choices are already sensible:

- dedup by insert-first unique key
- event-indexed bet lookup
- direct audit status updates
- integrity constraints
- clear persistence adapters

However, for **high load** and especially **horizontal scaling**, the biggest gaps are structural rather than cosmetic:

- the service needs a **shared durable database**
- the event flow needs **chunking/batching**
- the retry path needs **claim/lease semantics**
- the audit model needs **lifecycle management**
- pooling, batching, and transaction settings need to become explicit

If I had to summarize it in one line:

> The current DB layer is strong for a demo and correctness-focused assignment, but it must evolve from a process-local audit log into a shared, chunked, worker-safe persistence model before it can handle serious throughput or scale-out safely.
