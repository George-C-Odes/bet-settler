package io.github.georgecodes.betsettler.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PersistenceEntityTests {

  @Test
  void betEntityConstructorsAndGettersExposePersistedValues() {
    Instant createdAt = Instant.parse("2026-04-23T10:15:30Z");
    BetEntity entity =
        new BetEntity(
            "BET-1001",
            "USER-1",
            "EVT-1001",
            "MARKET-1",
            "TEAM-A",
            new BigDecimal("25.00"),
            createdAt);

    assertThat(entity.getBetId()).isEqualTo("BET-1001");
    assertThat(entity.getUserId()).isEqualTo("USER-1");
    assertThat(entity.getEventId()).isEqualTo("EVT-1001");
    assertThat(entity.getEventMarketId()).isEqualTo("MARKET-1");
    assertThat(entity.getEventWinnerId()).isEqualTo("TEAM-A");
    assertThat(entity.getBetAmount()).isEqualByComparingTo("25.00");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(new BetEntity().getBetId()).isNull();
  }

  @Test
  void processedEventOutcomeEntityConstructorsAndGettersExposePersistedValues() {
    Instant processedAt = Instant.parse("2026-04-23T10:20:30Z");
    ProcessedEventOutcomeEntity entity =
        new ProcessedEventOutcomeEntity("EVT-1001", "Team A vs Team B", "TEAM-A", processedAt);

    assertThat(entity.getEventId()).isEqualTo("EVT-1001");
    assertThat(entity.getEventName()).isEqualTo("Team A vs Team B");
    assertThat(entity.getEventWinnerId()).isEqualTo("TEAM-A");
    assertThat(entity.getProcessedAt()).isEqualTo(processedAt);
    assertThat(new ProcessedEventOutcomeEntity().getEventId()).isNull();
  }

  @Test
  void settlementAuditEntityTracksPendingFailedAndSentStates() {
    Instant createdAt = Instant.parse("2026-04-23T10:15:30Z");
    SettlementAuditEntity entity =
        new SettlementAuditEntity(
            "EVT-1001",
            "BET-1001",
            "USER-1",
            "WIN",
            "bet-settlements",
            "{\"betId\":\"BET-1001\"}",
            SettlementPublishStatus.PENDING,
            createdAt);

    assertThat(entity.getAuditId()).isNull();
    assertThat(entity.getEventId()).isEqualTo("EVT-1001");
    assertThat(entity.getBetId()).isEqualTo("BET-1001");
    assertThat(entity.getUserId()).isEqualTo("USER-1");
    assertThat(entity.getSettlementResult()).isEqualTo("WIN");
    assertThat(entity.getDestinationTopic()).isEqualTo("bet-settlements");
    assertThat(entity.getPayloadSnapshot()).isEqualTo("{\"betId\":\"BET-1001\"}");
    assertThat(entity.getPublishStatus()).isEqualTo(SettlementPublishStatus.PENDING);
    assertThat(entity.getPublishedAt()).isNull();
    assertThat(entity.getFailureReason()).isNull();
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);

    Instant failedAt = Instant.parse("2026-04-23T10:16:30Z");
    entity.markFailed(failedAt, "RocketMQ unavailable");
    assertThat(entity.getPublishStatus()).isEqualTo(SettlementPublishStatus.FAILED);
    assertThat(entity.getPublishedAt()).isEqualTo(failedAt);
    assertThat(entity.getFailureReason()).isEqualTo("RocketMQ unavailable");

    Instant sentAt = Instant.parse("2026-04-23T10:17:30Z");
    entity.markSent(sentAt);
    assertThat(entity.getPublishStatus()).isEqualTo(SettlementPublishStatus.SENT);
    assertThat(entity.getPublishedAt()).isEqualTo(sentAt);
    assertThat(entity.getFailureReason()).isNull();
    assertThat(new SettlementAuditEntity().getAuditId()).isNull();
  }
}
