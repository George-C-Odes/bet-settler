# Quality and Verification

This repository treats documentation, tests, static analysis, coverage, and Javadoc as part of the normal build.
The main local confidence command is:

```bat
.\mvnw.cmd clean verify
```

## Quality gates enforced by Maven

The `pom.xml` build is intentionally strict.

| Build phase       | Tool / plugin                            | What it enforces                                                           |
|-------------------|------------------------------------------|----------------------------------------------------------------------------|
| `validate`        | Spotless                                 | Formatting for Java, Markdown, XML, YAML, and selected repo files          |
| `validate`        | custom compact-constructor Javadoc check | Ensures record compact constructors keep parameter Javadoc aligned         |
| `test` / `verify` | Surefire + Spring Boot test stack        | Unit, slice, and integration tests                                         |
| `verify`          | PMD + CPD                                | Rule violations and copy-paste detection                                   |
| `verify`          | SpotBugs + FindSecBugs                   | Bug patterns and security-focused static analysis                          |
| `verify`          | JaCoCo report + check                    | Coverage reports and threshold enforcement                                 |
| `verify`          | Maven Javadoc plugin                     | Javadoc jar generation with `doclint=all` and warnings treated as failures |

## Coverage safeguards from `pom.xml`

JaCoCo does more than publish a report; it also fails the build when safeguards are missed.

| Scope                                      | Line minimum | Branch minimum |
|--------------------------------------------|-------------:|---------------:|
| Whole bundle                               |          96% |            91% |
| `domain` + selected `application` packages |          96% |            91% |
| selected `infrastructure` packages         |          95% |            90% |

The protected application-side package set currently includes:

- `io.github.georgecodes.betsettler.domain.model`
- `io.github.georgecodes.betsettler.domain.validation`
- `io.github.georgecodes.betsettler.domain.service`
- `io.github.georgecodes.betsettler.application.dto`
- `io.github.georgecodes.betsettler.application.model.audit`
- `io.github.georgecodes.betsettler.application.model.dispatch`
- `io.github.georgecodes.betsettler.application.port.out`
- `io.github.georgecodes.betsettler.application.service`

The protected infrastructure-side package set currently includes:

- `io.github.georgecodes.betsettler.infrastructure.config`
- `io.github.georgecodes.betsettler.infrastructure.observability`
- `io.github.georgecodes.betsettler.infrastructure.rest.controller`
- `io.github.georgecodes.betsettler.infrastructure.rest.dto`
- `io.github.georgecodes.betsettler.infrastructure.rest.error`
- `io.github.georgecodes.betsettler.infrastructure.rest.mapper`
- `io.github.georgecodes.betsettler.infrastructure.persistence.adapter`
- `io.github.georgecodes.betsettler.infrastructure.persistence.entity`
- `io.github.georgecodes.betsettler.infrastructure.persistence.mapper`
- `io.github.georgecodes.betsettler.infrastructure.messaging.kafka.consumer`
- `io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto`
- `io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper`
- `io.github.georgecodes.betsettler.infrastructure.messaging.kafka.producer`
- `io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto`
- `io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper`
- `io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload`
- `io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher`
- `io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher`

Within that protected REST scope, `infrastructure.rest.mapper` covers controller-focused mapper classes rather than one umbrella mapper, so coverage stays aligned with the split event-outcome, bet-query, and demo-helper adapter responsibilities.

## Recommended local verification sequence

```bat
.\mvnw.cmd spotless:check
.\mvnw.cmd verify
.\mvnw.cmd -q -B test-compile
.\mvnw.cmd clean verify
```

The latest passing `clean verify` snapshot records `196` test cases across `56` focused suites, and the strict Maven quality gates still pass end to end. That snapshot includes dedicated `PreparedDispatchPlan` contract coverage, tighter retry-path checks for replay eligibility and stored payload replay semantics, publisher-selection coverage that exercises the split dispatch/replay outbound contracts, focused payload-area coverage that separates snapshot-factory behavior from replay codec behavior.

Useful formatting helper:

```bat
.\mvnw.cmd spotless:apply
```

## CI command alignment

The repository intentionally uses a small command matrix rather than one single CI command everywhere:

| Context                  | Command                     | Why it exists                                                                                              |
|--------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------|
| local formatting gate    | `./mvnw spotless:check`     | fastest way to catch formatting drift before running the heavier checks                                    |
| local/CI verification    | `./mvnw -q -B verify`       | the Java coverage workflow's main gate; runs tests, static analysis, JaCoCo checks, and Javadoc generation |
| Qodana preflight         | `./mvnw -B -q test-compile` | matches the Qodana workflow and ensures both main and test sources compile before the IDE-style scan       |
| final local handoff gate | `./mvnw clean verify`       | rebuilds from a clean target directory and is the required repository handoff check                        |

## GitHub Actions workflows

### `java_coverage.yml`

Purpose: run the normal Maven verification gate and publish a readable summary plus coverage/test artifacts.

#### Triggers

- pull requests touching `src/**`, `config/quality/**`, `pom.xml`, the main support docs (`README.md`, `STRUCTURE.md`, `ARCHITECTURE.md`, `QUALITY.md`, `HELP.md`), `utils/test-stats/**`, or the workflow itself
- pushes to `main` for the same paths
- manual dispatch

#### What it does

1. checks out the repository
2. sets up Temurin JDK 25
3. runs `./mvnw -q -B verify`
4. parses Surefire and JaCoCo outputs with an inline Python summary script
5. uploads:
   - `target/jacoco-report/`
   - `target/site/jacoco/` when present
   - `target/surefire-reports/`

#### Why it matters

Because the workflow runs Maven `verify`, it implicitly exercises:

- Spotless
- compact-constructor Javadoc validation
- tests
- PMD / CPD
- SpotBugs / FindSecBugs
- JaCoCo thresholds
- Javadoc generation

### `qodana_code_quality.yml`

Purpose: run JetBrains Qodana as an additional IDE-style static-analysis gate.

#### Triggers

- pull requests touching `src/**`, `config/quality/**`, `pom.xml`, the main support docs (`README.md`, `STRUCTURE.md`, `ARCHITECTURE.md`, `QUALITY.md`, `HELP.md`), `utils/test-stats/**`, `qodana.yaml`, the workflow itself, or an optional baseline under `.qodana/baseline/**`
- pushes to `main` for the same paths
- manual dispatch

#### What it does

1. checks out the full repository history
2. sets up Temurin JDK 25
3. runs `./mvnw -B -q test-compile`
4. prepares optional baseline arguments if `.qodana/baseline/project.sarif.json` exists
5. runs `jetbrains/qodana-jvm-community:2025.3`
6. uploads Qodana results and the HTML report artifact

#### Effective Qodana policy

From `qodana.yaml`:

- profile: `qodana.recommended`
- custom checks include naming conventions, missing Javadoc, logging misuse, nullability, and unnecessary fully qualified names
- excluded noisy paths include `.github`, `config`, `target`, and `utils`
- `MissingJavadoc` is excluded for test sources
- failure thresholds are strict: `critical=0`, `high=0`, `moderate=0`

## Current committed verification snapshot

| Metric                               |      Current value |
|--------------------------------------|-------------------:|
| Test suites                          |                 56 |
| Test cases                           |                196 |
| Failures                             |                  0 |
| Errors                               |                  0 |
| Skipped                              |                  0 |
| Total recorded test time             |           74.819 s |
| Line coverage                        |    99.8% (910/912) |
| Branch coverage                      |    98.1% (104/106) |
| Instruction coverage                 |  99.8% (3557/3565) |
| Method coverage                      |   100.0% (215/215) |
| Domain + application line coverage   |    99.3% (303/305) |
| Domain + application branch coverage |      97.4% (37/38) |
| Infrastructure line coverage         |   100.0% (593/593) |
| Infrastructure branch coverage       |      98.4% (61/62) |
| SpotBugs findings                    |                  0 |

## Generated report locations

After a successful build, the most useful outputs are:

- `target/jacoco-report/` - HTML, XML, and CSV coverage reports
- `target/surefire-reports/` - XML and text test results
- `target/spotbugsXml.xml` - SpotBugs machine-readable report
- `target/pmd.xml` and `target/cpd.xml` - PMD and duplicate-code reports
- `target/bet-settler-0.0.1-SNAPSHOT-javadoc.jar` - generated Javadoc artifact

## How to refresh `utils/test-stats/stats.txt`

The repo already includes a small PowerShell helper at `utils/test-stats/extractor.ps1`.
It reads reports from the repository root, mirrors the same protected package scopes configured in `pom.xml` including the application model audit/dispatch packages, the neutral `infrastructure.messaging.settlement.*` packages, and the RocketMQ publisher package, and writes directly back to the committed `utils/test-stats/stats.txt` file.

1. run a fresh verification build:

   ```bat
   .\mvnw.cmd clean verify
   ```

2. refresh the committed stats file from the generated reports:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\utils\test-stats\extractor.ps1
   ```

   IntelliJ users can run the checked-in `Produce Test Stats` configuration in `.run/` for the same command.

3. review the updated `utils/test-stats/stats.txt`
4. if coverage or counts changed meaningfully, copy the refreshed highlights into this document

## Interpreting failures quickly

If `verify` fails, check these in order:

1. `target/surefire-reports/` for test failures
2. Spotless formatting output
3. PMD / CPD violations
4. SpotBugs / FindSecBugs findings
5. JaCoCo threshold failures
6. Javadoc warnings promoted to errors

## Related documents

- [`README.md`](README.md) - practical entry guide
- [`STRUCTURE.md`](STRUCTURE.md) - repository layout and layer ownership
- [`ARCHITECTURE.md`](ARCHITECTURE.md) - runtime flow and database/message design
