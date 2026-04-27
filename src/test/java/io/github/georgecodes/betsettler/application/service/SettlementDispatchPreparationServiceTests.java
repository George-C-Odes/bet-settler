package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.dispatch.PreparedDispatchPlan;
import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementPayloadPort;
import io.github.georgecodes.betsettler.application.port.out.ProcessedEventOutcomePort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.domain.service.BetSettlementDecider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementDispatchPreparationServiceTests {

  private static final Instant CREATED_AT = Instant.parse("2026-04-23T10:15:30Z");

  @Mock private BetQueryPort betQueryPort;

  @Mock private ProcessedEventOutcomePort processedEventOutcomePort;

  @Mock private SettlementAuditPort settlementAuditPort;

  @Mock private BetSettlementPayloadPort betSettlementPayloadPort;

  @Mock private BetSettlementDecider betSettlementDecider;

  private SettlementDispatchPreparationService settlementDispatchPreparationService;

  @BeforeEach
  void setUp() {
    settlementDispatchPreparationService =
        new SettlementDispatchPreparationService(
            betQueryPort,
            processedEventOutcomePort,
            settlementAuditPort,
            betSettlementPayloadPort,
            betSettlementDecider);
  }

  @Test
  void prepareSkipsDuplicateOutcomesBeforeLoadingBets() {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    when(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).thenReturn(false);

    PreparedDispatchPlan preparedDispatchPlan =
        settlementDispatchPreparationService.prepare(eventOutcome);

    assertThat(preparedDispatchPlan).isEqualTo(PreparedDispatchPlan.duplicate("EVT-1001"));
    assertThat(preparedDispatchPlan.status()).isEqualTo(PreparedDispatchPlan.Status.DUPLICATE);
    assertThat(preparedDispatchPlan.hasDestinationTopic()).isFalse();
    verify(processedEventOutcomePort).recordProcessedIfAbsent(eventOutcome);
    verify(betQueryPort, never()).getBetsByEventId(any());
    verify(settlementAuditPort, never()).savePendingSettlements(any());
  }

  @Test
  void prepareCreatesPendingAuditsForAllPreparedSettlements() {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    Bet winningBet = createBet("BET-1001", "USER-1", "TEAM-A");
    Bet losingBet = createBet("BET-1002", "USER-2", "TEAM-B");
    BetSettlement winningSettlement =
        new BetSettlement(winningBet, eventOutcome, SettlementResult.WIN);
    BetSettlement losingSettlement =
        new BetSettlement(losingBet, eventOutcome, SettlementResult.LOSE);
    when(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).thenReturn(true);
    when(betSettlementPayloadPort.destinationTopic()).thenReturn("bet-settlements");
    when(betQueryPort.getBetsByEventId("EVT-1001")).thenReturn(List.of(winningBet, losingBet));
    when(betSettlementDecider.settle(winningBet, eventOutcome)).thenReturn(winningSettlement);
    when(betSettlementDecider.settle(losingBet, eventOutcome)).thenReturn(losingSettlement);
    when(betSettlementPayloadPort.payloadSnapshot(winningSettlement))
        .thenReturn("{\"betId\":\"BET-1001\"}");
    when(betSettlementPayloadPort.payloadSnapshot(losingSettlement))
        .thenReturn("{\"betId\":\"BET-1002\"}");

    PreparedDispatchPlan preparedDispatchPlan =
        settlementDispatchPreparationService.prepare(eventOutcome);

    assertThat(preparedDispatchPlan.eventId()).isEqualTo("EVT-1001");
    assertThat(preparedDispatchPlan.status())
        .isEqualTo(PreparedDispatchPlan.Status.DISPATCH_REQUIRED);
    assertThat(preparedDispatchPlan.requiredDestinationTopic()).isEqualTo("bet-settlements");
    assertThat(preparedDispatchPlan.settlements())
        .containsExactly(winningSettlement, losingSettlement);
    assertThat(preparedDispatchPlan.requiresDispatch()).isTrue();
    verify(processedEventOutcomePort).recordProcessedIfAbsent(eventOutcome);
    verify(betSettlementDecider).settle(winningBet, eventOutcome);
    verify(betSettlementDecider).settle(losingBet, eventOutcome);
    verify(settlementAuditPort)
        .savePendingSettlements(
            List.of(
                new PendingSettlementAudit(
                    winningSettlement, "bet-settlements", "{\"betId\":\"BET-1001\"}"),
                new PendingSettlementAudit(
                    losingSettlement, "bet-settlements", "{\"betId\":\"BET-1002\"}")));
  }

  @Test
  void prepareTreatsAtomicProcessedInsertRaceAsDuplicate() {
    EventOutcome eventOutcome = new EventOutcome("EVT-4001", "Team G vs Team H", "TEAM-G");
    when(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).thenReturn(false);

    PreparedDispatchPlan preparedDispatchPlan =
        settlementDispatchPreparationService.prepare(eventOutcome);

    assertThat(preparedDispatchPlan).isEqualTo(PreparedDispatchPlan.duplicate("EVT-4001"));
    assertThat(preparedDispatchPlan.hasDestinationTopic()).isFalse();
    verify(betQueryPort, never()).getBetsByEventId(any());
    verify(settlementAuditPort, never()).savePendingSettlements(any());
  }

  @Test
  void prepareReturnsNoMatchPlanWhenNoBetsAreFound() {
    EventOutcome eventOutcome = new EventOutcome("EVT-9999", "Team X vs Team Y", "TEAM-X");
    when(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).thenReturn(true);
    when(betSettlementPayloadPort.destinationTopic()).thenReturn("bet-settlements");
    when(betQueryPort.getBetsByEventId("EVT-9999")).thenReturn(List.of());

    PreparedDispatchPlan preparedDispatchPlan =
        settlementDispatchPreparationService.prepare(eventOutcome);

    assertThat(preparedDispatchPlan.status()).isEqualTo(PreparedDispatchPlan.Status.NO_MATCH);
    assertThat(preparedDispatchPlan.requiredDestinationTopic()).isEqualTo("bet-settlements");
    assertThat(preparedDispatchPlan.settlements()).isEmpty();
    assertThat(preparedDispatchPlan.requiresDispatch()).isFalse();
    verify(settlementAuditPort, never()).savePendingSettlements(any());
    verify(betSettlementDecider, never()).settle(any(), eq(eventOutcome));
  }

  @Test
  void prepareRejectsBlankDestinationTopic() {
    EventOutcome eventOutcome = new EventOutcome("EVT-7001", "Team M vs Team N", "TEAM-M");
    when(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).thenReturn(true);
    when(betSettlementPayloadPort.destinationTopic()).thenReturn("   ");

    assertThatThrownBy(() -> settlementDispatchPreparationService.prepare(eventOutcome))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("destinationTopic must not be blank");

    verify(betQueryPort, never()).getBetsByEventId(any());
    verify(settlementAuditPort, never()).savePendingSettlements(any());
  }

  private Bet createBet(String betId, String userId, String eventWinnerId) {
    return new Bet(
        betId, userId, "EVT-1001", "MARKET-1", eventWinnerId, new BigDecimal("25.00"), CREATED_AT);
  }
}
