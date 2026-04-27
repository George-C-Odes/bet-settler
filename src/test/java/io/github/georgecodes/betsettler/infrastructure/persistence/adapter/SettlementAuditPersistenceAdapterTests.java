package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementAuditPersistenceAdapterTests {

  private static final Instant CREATED_AT = Instant.parse("2026-04-23T10:15:30Z");

  @Mock private SpringDataSettlementAuditRepository settlementAuditRepository;

  private SettlementAuditPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new SettlementAuditPersistenceAdapter(
            settlementAuditRepository, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
  }

  @Test
  void savePendingSettlementsSkipsRepositoryCallWhenListIsEmpty() {
    adapter.savePendingSettlements(List.of());

    verify(settlementAuditRepository, never()).saveAll(any());
  }

  @Test
  void savePendingSettlementsPersistsAllEntriesInOneBatch() {
    BetSettlement winningSettlement = createSettlement("BET-1001", "USER-1", SettlementResult.WIN);
    BetSettlement losingSettlement = createSettlement("BET-1002", "USER-2", SettlementResult.LOSE);

    adapter.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(
                winningSettlement, "bet-settlements", "{\"betId\":\"BET-1001\"}"),
            new PendingSettlementAudit(
                losingSettlement, "bet-settlements", "{\"betId\":\"BET-1002\"}")));

    verify(settlementAuditRepository)
        .saveAll(
            assertArg(
                savedEntities ->
                    assertThat(savedEntities)
                        .extracting(
                            SettlementAuditEntity::getBetId,
                            SettlementAuditEntity::getDestinationTopic,
                            SettlementAuditEntity::getPublishStatus,
                            SettlementAuditEntity::getCreatedAt)
                        .containsExactly(
                            tuple(
                                "BET-1001",
                                "bet-settlements",
                                SettlementPublishStatus.PENDING,
                                CREATED_AT),
                            tuple(
                                "BET-1002",
                                "bet-settlements",
                                SettlementPublishStatus.PENDING,
                                CREATED_AT))));
  }

  @Test
  void findRetryableSettlementsReturnsPendingAndFailedRowsInReplayOrder() {
    SettlementAuditEntity pendingAudit =
        createAuditEntity(
            "EVT-1001", "BET-1001", "USER-1", SettlementPublishStatus.PENDING, CREATED_AT);
    SettlementAuditEntity failedAudit =
        createAuditEntity(
            "EVT-1002",
            "BET-1002",
            "USER-2",
            SettlementPublishStatus.FAILED,
            CREATED_AT.plusSeconds(30));
    when(settlementAuditRepository.findAllByPublishStatusInOrderByCreatedAtAscBetIdAsc(
            List.of(SettlementPublishStatus.PENDING, SettlementPublishStatus.FAILED)))
        .thenReturn(List.of(pendingAudit, failedAudit));

    List<RetryableSettlementAudit> retryableSettlements = adapter.findRetryableSettlements();

    assertThat(retryableSettlements)
        .containsExactly(
            new RetryableSettlementAudit(
                "EVT-1001",
                "BET-1001",
                "USER-1",
                "PENDING",
                "bet-settlements",
                "{\"betId\":\"BET-1001\"}"),
            new RetryableSettlementAudit(
                "EVT-1002",
                "BET-1002",
                "USER-2",
                "FAILED",
                "bet-settlements",
                "{\"betId\":\"BET-1002\"}"));
  }

  @Test
  void markSentNormalizesIdentifiersAndUpdatesPublishState() {
    when(settlementAuditRepository.updatePublishState(
            eq("EVT-1001"),
            eq("BET-1001"),
            eq(SettlementPublishStatus.SENT),
            eq(CREATED_AT),
            eq(null)))
        .thenReturn(1);

    adapter.markSent(" EVT-1001 ", " BET-1001 ");

    verify(settlementAuditRepository)
        .updatePublishState(
            eq("EVT-1001"),
            eq("BET-1001"),
            eq(SettlementPublishStatus.SENT),
            eq(CREATED_AT),
            eq(null));
  }

  @Test
  void markFailedNormalizesFailureReasonAndUpdatesPublishState() {
    when(settlementAuditRepository.updatePublishState(
            eq("EVT-2001"),
            eq("BET-2001"),
            eq(SettlementPublishStatus.FAILED),
            eq(CREATED_AT),
            eq("RocketMQ unavailable")))
        .thenReturn(1);

    adapter.markFailed(" EVT-2001 ", " BET-2001 ", " RocketMQ unavailable ");

    verify(settlementAuditRepository)
        .updatePublishState(
            eq("EVT-2001"),
            eq("BET-2001"),
            eq(SettlementPublishStatus.FAILED),
            eq(CREATED_AT),
            eq("RocketMQ unavailable"));
  }

  @Test
  void markSentRejectsUnknownAudit() {
    when(settlementAuditRepository.updatePublishState(
            eq("EVT-9999"),
            eq("BET-9999"),
            eq(SettlementPublishStatus.SENT),
            eq(CREATED_AT),
            eq(null)))
        .thenReturn(0);

    assertThatIllegalStateException()
        .isThrownBy(() -> adapter.markSent("EVT-9999", "BET-9999"))
        .withMessage("No settlement audit exists for eventId=EVT-9999 and betId=BET-9999");
  }

  private BetSettlement createSettlement(String betId, String userId, SettlementResult result) {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    Bet bet =
        new Bet(
            betId,
            userId,
            "EVT-1001",
            "MARKET-1",
            result == SettlementResult.WIN ? "TEAM-A" : "TEAM-B",
            new BigDecimal("25.00"),
            CREATED_AT);
    return new BetSettlement(bet, eventOutcome, result);
  }

  private SettlementAuditEntity createAuditEntity(
      String eventId,
      String betId,
      String userId,
      SettlementPublishStatus publishStatus,
      Instant createdAt) {
    return new SettlementAuditEntity(
        eventId,
        betId,
        userId,
        publishStatus == SettlementPublishStatus.PENDING ? "WIN" : "LOSE",
        "bet-settlements",
        "{\"betId\":\"" + betId + "\"}",
        publishStatus,
        createdAt);
  }
}
