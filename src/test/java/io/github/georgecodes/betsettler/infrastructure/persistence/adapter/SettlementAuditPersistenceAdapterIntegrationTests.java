package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettlementAuditPersistenceAdapterIntegrationTests {

  @Autowired private BetQueryPort betQueryPort;

  @Autowired private SettlementAuditPort settlementAuditPort;

  @Autowired private SpringDataSettlementAuditRepository settlementAuditRepository;

  @Test
  void savePendingSettlementPersistsPendingAuditRow() {
    BetSettlement settlement = createSettlement("BET-1001", "Team A vs Team B", "TEAM-A");

    settlementAuditPort.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(
                settlement, " bet-settlements ", " {\"betId\":\"BET-1001\"} ")));

    assertThat(settlementAuditRepository.findByEventIdAndBetId("EVT-1001", "BET-1001"))
        .get()
        .extracting(
            SettlementAuditEntity::getUserId,
            SettlementAuditEntity::getSettlementResult,
            SettlementAuditEntity::getDestinationTopic,
            SettlementAuditEntity::getPayloadSnapshot,
            SettlementAuditEntity::getPublishStatus,
            SettlementAuditEntity::getPublishedAt,
            SettlementAuditEntity::getFailureReason)
        .containsExactly(
            "USER-1",
            "WIN",
            "bet-settlements",
            "{\"betId\":\"BET-1001\"}",
            SettlementPublishStatus.PENDING,
            null,
            null);
  }

  @Test
  void findRetryableSettlementsReturnsOnlyPendingAndFailedRows() {
    BetSettlement pendingSettlement = createSettlement("BET-1001", "Team A vs Team B", "TEAM-A");
    BetSettlement failedSettlement = createSettlement("BET-1002", "Team A vs Team B", "TEAM-A");
    BetSettlement sentSettlement = createSettlement("BET-3001", "Team E vs Team F", "TEAM-D");

    settlementAuditPort.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(
                pendingSettlement, "bet-settlements", "{\"betId\":\"BET-1001\"}"),
            new PendingSettlementAudit(
                failedSettlement, "bet-settlements", "{\"betId\":\"BET-1002\"}"),
            new PendingSettlementAudit(
                sentSettlement, "bet-settlements", "{\"betId\":\"BET-3001\"}")));
    settlementAuditPort.markFailed("EVT-1001", "BET-1002", "RocketMQ unavailable");
    settlementAuditPort.markSent("EVT-3001", "BET-3001");

    List<RetryableSettlementAudit> retryableSettlements =
        settlementAuditPort.findRetryableSettlements();

    assertThat(retryableSettlements)
        .extracting(
            RetryableSettlementAudit::eventId,
            RetryableSettlementAudit::betId,
            RetryableSettlementAudit::publishStatus,
            RetryableSettlementAudit::destinationTopic,
            RetryableSettlementAudit::payloadSnapshot)
        .containsExactly(
            tuple("EVT-1001", "BET-1001", "PENDING", "bet-settlements", "{\"betId\":\"BET-1001\"}"),
            tuple("EVT-1001", "BET-1002", "FAILED", "bet-settlements", "{\"betId\":\"BET-1002\"}"));
  }

  @Test
  void markSentUpdatesPendingAuditStatus() {
    BetSettlement settlement = createSettlement("BET-1002", "Team A vs Team B", "TEAM-A");
    settlementAuditPort.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(settlement, "bet-settlements", "{\"betId\":\"BET-1002\"}")));

    settlementAuditPort.markSent(" EVT-1001 ", " BET-1002 ");

    assertThat(settlementAuditRepository.findByEventIdAndBetId("EVT-1001", "BET-1002"))
        .get()
        .extracting(
            SettlementAuditEntity::getPublishStatus,
            SettlementAuditEntity::getPublishedAt,
            SettlementAuditEntity::getFailureReason)
        .containsExactly(
            SettlementPublishStatus.SENT,
            settlementAuditRepository
                .findByEventIdAndBetId("EVT-1001", "BET-1002")
                .orElseThrow()
                .getPublishedAt(),
            null);
  }

  @Test
  void markFailedUpdatesPendingAuditStatusAndReason() {
    BetSettlement settlement = createSettlement("BET-2001", "Team C vs Team D", "TEAM-D");
    settlementAuditPort.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(settlement, "bet-settlements", "{\"betId\":\"BET-2001\"}")));

    settlementAuditPort.markFailed("EVT-2001", "BET-2001", " RocketMQ unavailable ");

    assertThat(settlementAuditRepository.findByEventIdAndBetId("EVT-2001", "BET-2001"))
        .get()
        .extracting(
            SettlementAuditEntity::getPublishStatus,
            SettlementAuditEntity::getFailureReason,
            SettlementAuditEntity::getPublishedAt)
        .containsExactly(
            SettlementPublishStatus.FAILED,
            "RocketMQ unavailable",
            settlementAuditRepository
                .findByEventIdAndBetId("EVT-2001", "BET-2001")
                .orElseThrow()
                .getPublishedAt());
  }

  @Test
  void savePendingSettlementsPersistsMultiplePendingAuditRows() {
    BetSettlement winningSettlement = createSettlement("BET-1001", "Team A vs Team B", "TEAM-A");
    BetSettlement losingSettlement = createSettlement("BET-1002", "Team A vs Team B", "TEAM-A");

    settlementAuditPort.savePendingSettlements(
        List.of(
            new PendingSettlementAudit(
                winningSettlement, "bet-settlements", "{\"betId\":\"BET-1001\"}"),
            new PendingSettlementAudit(
                losingSettlement, "bet-settlements", "{\"betId\":\"BET-1002\"}")));

    assertThat(settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-1001"))
        .extracting(
            SettlementAuditEntity::getBetId,
            SettlementAuditEntity::getDestinationTopic,
            SettlementAuditEntity::getPublishStatus)
        .containsExactly(
            tuple("BET-1001", "bet-settlements", SettlementPublishStatus.PENDING),
            tuple("BET-1002", "bet-settlements", SettlementPublishStatus.PENDING));
  }

  @Test
  void markSentRejectsUnknownAudit() {
    assertThatIllegalStateException()
        .isThrownBy(() -> settlementAuditPort.markSent("EVT-9999", "BET-9999"))
        .withMessage("No settlement audit exists for eventId=EVT-9999 and betId=BET-9999");
  }

  private BetSettlement createSettlement(String betId, String eventName, String eventWinnerId) {
    Bet bet =
        betQueryPort.getAllBets().stream()
            .filter(candidateBet -> candidateBet.betId().equals(betId))
            .findFirst()
            .orElseThrow();
    EventOutcome eventOutcome = new EventOutcome(bet.eventId(), eventName, eventWinnerId);
    SettlementResult settlementResult =
        bet.eventWinnerId().equals(eventWinnerId) ? SettlementResult.WIN : SettlementResult.LOSE;
    return new BetSettlement(bet, eventOutcome, settlementResult);
  }
}
