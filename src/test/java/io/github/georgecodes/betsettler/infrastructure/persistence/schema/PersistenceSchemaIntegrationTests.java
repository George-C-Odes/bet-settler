package io.github.georgecodes.betsettler.infrastructure.persistence.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.infrastructure.persistence.entity.BetEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataBetRepository;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenceSchemaIntegrationTests {

  //noinspection SqlNoDataSourceInspection,SqlResolve
  private static final String TABLE_COUNT_SQL =
      """
      SELECT COUNT(*)
      FROM INFORMATION_SCHEMA.TABLES
      WHERE TABLE_SCHEMA = 'PUBLIC'
        AND TABLE_NAME IN ('BET', 'PROCESSED_EVENT_OUTCOME', 'SETTLEMENT_AUDIT')
      """;

  //noinspection SqlNoDataSourceInspection,SqlResolve
  private static final String INSERT_PROCESSED_EVENT_OUTCOME_SQL =
      """
      INSERT INTO processed_event_outcome (event_id, event_name, event_winner_id, processed_at)
      VALUES (?, ?, ?, ?)
      """;

  //noinspection SqlNoDataSourceInspection,SqlResolve
  private static final String INSERT_BET_SQL =
      """
      INSERT INTO bet (
          bet_id,
          user_id,
          event_id,
          event_market_id,
          event_winner_id,
          bet_amount,
          created_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """;

  //noinspection SqlNoDataSourceInspection,SqlResolve
  private static final String INSERT_SETTLEMENT_AUDIT_SQL =
      """
      INSERT INTO settlement_audit (
          event_id,
          bet_id,
          user_id,
          settlement_result,
          destination_topic,
          payload_snapshot,
          publish_status,
          created_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private SpringDataBetRepository betRepository;

  @Test
  void flywayCreatesExpectedTables() {
    Integer createdTableCount = jdbcTemplate.queryForObject(TABLE_COUNT_SQL, Integer.class);

    assertThat(createdTableCount).isEqualTo(3);
  }

  @Test
  void seedDataCoversWinningLosingAndNoMatchQueries() {
    assertThat(betRepository.findAllByOrderByBetIdAsc())
        .extracting(BetEntity::getBetId)
        .containsExactly("BET-1001", "BET-1002", "BET-2001", "BET-3001");

    assertThat(betRepository.findByEventIdOrderByBetIdAsc("EVT-1001"))
        .extracting(BetEntity::getEventWinnerId)
        .containsExactly("TEAM-A", "TEAM-B");

    assertThat(betRepository.findByEventIdOrderByBetIdAsc("EVT-9999")).isEmpty();
  }

  @Test
  void flywayCreatesCompositeBetLookupIndexForEventOrderedQueries() {
    Set<String> indexNames = getNamedIndexColumns("BET").keySet();

    assertThat(indexNames).containsExactly("IDX_BET_EVENT_ID_BET_ID");
  }

  @Test
  void flywayCreatesRetryableSettlementAuditIndexForReplayQueries() {
    Map<String, List<String>> indexColumnsByName = getNamedIndexColumns("SETTLEMENT_AUDIT");

    assertThat(indexColumnsByName)
        .containsEntry(
            "IDX_SETTLEMENT_AUDIT_STATUS_CREATED_BET",
            List.of("PUBLISH_STATUS", "CREATED_AT", "BET_ID"));
    assertThat(indexColumnsByName.keySet())
        .containsExactly("IDX_SETTLEMENT_AUDIT_STATUS_CREATED_BET");
  }

  @Test
  void flywayUsesBoundedVarcharForSettlementAuditPayloadSnapshots() {
    ColumnMetadata payloadSnapshotColumn = getSettlementAuditPayloadSnapshotColumnMetadata();

    assertThat(payloadSnapshotColumn.typeName()).matches("(?i)(character varying|varchar)");
    assertThat(payloadSnapshotColumn.columnSize()).isEqualTo(4096);
  }

  @Test
  void processedEventOutcomeTableRejectsDuplicateEventIdentifiers() {
    jdbcTemplate.update(
        INSERT_PROCESSED_EVENT_OUTCOME_SQL,
        "EVT-5001",
        "Team G vs Team H",
        "TEAM-G",
        Timestamp.from(Instant.parse("2026-04-23T09:15:30Z")));

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    INSERT_PROCESSED_EVENT_OUTCOME_SQL,
                    "EVT-5001",
                    "Team G vs Team H Rematch",
                    "TEAM-H",
                    Timestamp.from(Instant.parse("2026-04-23T09:16:30Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void settlementAuditTableRejectsDuplicateEventAndBetPairs() {
    jdbcTemplate.update(
        INSERT_SETTLEMENT_AUDIT_SQL,
        "EVT-1001",
        "BET-1001",
        "USER-1",
        "WIN",
        "bet-settlements",
        "{\"betId\":\"BET-1001\"}",
        "PENDING",
        Timestamp.from(Instant.parse("2026-04-23T09:20:30Z")));

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    INSERT_SETTLEMENT_AUDIT_SQL,
                    "EVT-1001",
                    "BET-1001",
                    "USER-1",
                    "WIN",
                    "bet-settlements",
                    "{\"betId\":\"BET-1001\",\"duplicate\":true}",
                    "PENDING",
                    Timestamp.from(Instant.parse("2026-04-23T09:21:30Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void settlementAuditTableAcceptsPayloadSnapshotUpToConfiguredBound() {
    insertBet("BET-4001", "EVT-4001", "12.50");
    String payloadSnapshot = "x".repeat(4096);

    jdbcTemplate.update(
        INSERT_SETTLEMENT_AUDIT_SQL,
        "EVT-4001",
        "BET-4001",
        "USER-40",
        "WIN",
        "bet-settlements",
        payloadSnapshot,
        "PENDING",
        Timestamp.from(Instant.parse("2026-04-23T09:22:30Z")));

    Integer payloadLength =
        jdbcTemplate.queryForObject(
            "SELECT LENGTH(payload_snapshot) FROM settlement_audit WHERE event_id = ? AND bet_id = ?",
            Integer.class,
            "EVT-4001",
            "BET-4001");

    assertThat(payloadLength).isEqualTo(4096);
  }

  @Test
  void settlementAuditTableRejectsPayloadSnapshotsAboveConfiguredBound() {
    insertBet("BET-4002", "EVT-4002", "18.75");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    INSERT_SETTLEMENT_AUDIT_SQL,
                    "EVT-4002",
                    "BET-4002",
                    "USER-41",
                    "LOSE",
                    "bet-settlements",
                    "x".repeat(4097),
                    "PENDING",
                    Timestamp.from(Instant.parse("2026-04-23T09:23:30Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void betTableRejectsNonPositiveAmounts() {
    assertThatThrownBy(() -> insertBet("BET-4003", "EVT-4003", "0.00"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void settlementAuditTableRejectsUnsupportedSettlementResult() {
    insertBet("BET-4004", "EVT-4004", "7.50");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    INSERT_SETTLEMENT_AUDIT_SQL,
                    "EVT-4004",
                    "BET-4004",
                    "USER-42",
                    "DRAW",
                    "bet-settlements",
                    "{\"betId\":\"BET-4004\"}",
                    "PENDING",
                    Timestamp.from(Instant.parse("2026-04-23T09:24:30Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void settlementAuditTableRejectsUnsupportedPublishStatus() {
    insertBet("BET-4005", "EVT-4005", "9.00");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    INSERT_SETTLEMENT_AUDIT_SQL,
                    "EVT-4005",
                    "BET-4005",
                    "USER-43",
                    "WIN",
                    "bet-settlements",
                    "{\"betId\":\"BET-4005\"}",
                    "QUEUED",
                    Timestamp.from(Instant.parse("2026-04-23T09:25:30Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Map<String, List<String>> getIndexColumns(String tableName) {
    return jdbcTemplate.execute(
        (ConnectionCallback<Map<String, List<String>>>)
            connection -> {
              DatabaseMetaData databaseMetaData = connection.getMetaData();
              Map<String, List<String>> indexColumnsByName = new LinkedHashMap<>();
              try (ResultSet resultSet =
                  databaseMetaData.getIndexInfo(null, "PUBLIC", tableName, false, false)) {
                while (resultSet.next()) {
                  String indexName = resultSet.getString("INDEX_NAME");
                  String columnName = resultSet.getString("COLUMN_NAME");
                  if (indexName != null && columnName != null) {
                    indexColumnsByName
                        .computeIfAbsent(indexName, ignored -> new ArrayList<>())
                        .add(columnName);
                  }
                }
              } catch (SQLException sqlException) {
                throw new IllegalStateException(
                    "Failed to read index metadata for table " + tableName, sqlException);
              }
              return indexColumnsByName;
            });
  }

  private Map<String, List<String>> getNamedIndexColumns(String tableName) {
    return getIndexColumns(tableName).entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("IDX_"))
        .collect(
            LinkedHashMap::new,
            (indexColumnsByName, entry) -> indexColumnsByName.put(entry.getKey(), entry.getValue()),
            LinkedHashMap::putAll);
  }

  private ColumnMetadata getSettlementAuditPayloadSnapshotColumnMetadata() {
    return jdbcTemplate.execute(
        (ConnectionCallback<ColumnMetadata>)
            connection -> {
              DatabaseMetaData databaseMetaData = connection.getMetaData();
              try (ResultSet resultSet =
                  databaseMetaData.getColumns(
                      null, "PUBLIC", "SETTLEMENT_AUDIT", "PAYLOAD_SNAPSHOT")) {
                if (!resultSet.next()) {
                  throw new IllegalStateException(
                      "Column SETTLEMENT_AUDIT.PAYLOAD_SNAPSHOT does not exist");
                }
                return new ColumnMetadata(
                    resultSet.getString("TYPE_NAME"), resultSet.getInt("COLUMN_SIZE"));
              } catch (SQLException sqlException) {
                throw new IllegalStateException(
                    "Failed to read column metadata for SETTLEMENT_AUDIT.PAYLOAD_SNAPSHOT",
                    sqlException);
              }
            });
  }

  private void insertBet(String betId, String eventId, String amount) {
    jdbcTemplate.update(
        INSERT_BET_SQL,
        betId,
        "USER-" + betId,
        eventId,
        "MARKET-" + eventId,
        "TEAM-" + eventId,
        amount,
        Timestamp.from(Instant.parse("2026-04-23T09:10:30Z")));
  }

  private record ColumnMetadata(String typeName, int columnSize) {}
}
