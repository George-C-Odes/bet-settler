package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.model.dispatch.PreparedDispatchPlan;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.github.georgecodes.betsettler.application.support.SettlementFailureReasonFormatter;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class ProcessEventOutcomeServiceTests {

  private static final Instant CREATED_AT = Instant.parse("2026-04-23T10:15:30Z");

  @Mock private TransactionOperations transactionOperations;

  @Mock private SettlementDispatchPreparationService settlementDispatchPreparationService;

  @Mock private SettlementAuditPort settlementAuditPort;

  @Mock private BetSettlementDispatchPort betSettlementDispatchPort;

  @Mock private SettlementFlowMetricsPort settlementFlowMetricsPort;

  private ProcessEventOutcomeService processEventOutcomeService;

  private final SettlementFailureReasonFormatter settlementFailureReasonFormatter =
      new SettlementFailureReasonFormatter();

  @BeforeEach
  void setUp() {
    processEventOutcomeService =
        new ProcessEventOutcomeService(
            transactionOperations,
            settlementDispatchPreparationService,
            settlementAuditPort,
            betSettlementDispatchPort,
            settlementFailureReasonFormatter,
            settlementFlowMetricsPort);
  }

  @Test
  void processSkipsDuplicateOutcomesBeforePreparingSettlements() {
    when(transactionOperations.execute(any())).thenAnswer(this::executeTransactionCallback);
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(PreparedDispatchPlan.duplicate("EVT-1001"));

    processEventOutcomeService.process(eventOutcome);

    verify(settlementDispatchPreparationService).prepare(eventOutcome);
    verify(betSettlementDispatchPort, never()).publish(any());
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DUPLICATE);
  }

  @Test
  void processCreatesPendingAuditsAndPublishesAllMatchingSettlements() {
    when(transactionOperations.execute(any())).thenAnswer(this::executeTransactionCallback);
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    BetSettlement winningSettlement =
        createSettlement("BET-1001", "USER-1", eventOutcome, "TEAM-A", SettlementResult.WIN);
    BetSettlement losingSettlement =
        createSettlement("BET-1002", "USER-2", eventOutcome, "TEAM-B", SettlementResult.LOSE);
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(
            PreparedDispatchPlan.dispatch(
                "EVT-1001", "bet-settlements", List.of(winningSettlement, losingSettlement)));

    processEventOutcomeService.process(eventOutcome);

    verify(betSettlementDispatchPort).publish(winningSettlement);
    verify(betSettlementDispatchPort).publish(losingSettlement);
    verify(settlementAuditPort).markSent("EVT-1001", "BET-1001");
    verify(settlementAuditPort).markSent("EVT-1001", "BET-1002");
    verify(settlementAuditPort, never()).markFailed(any(), any(), any());
    verify(settlementFlowMetricsPort).recordPreparedSettlements(2);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    verify(settlementFlowMetricsPort).recordSettlementDispatchCounts(2, 0);
  }

  @Test
  void processMarksAuditFailedWhenPublicationThrowsAndContinues() {
    when(transactionOperations.execute(any())).thenAnswer(this::executeTransactionCallback);
    EventOutcome eventOutcome = new EventOutcome("EVT-2001", "Team C vs Team D", "TEAM-D");
    BetSettlement losingSettlement =
        createSettlement("BET-2001", "USER-3", eventOutcome, "TEAM-C", SettlementResult.LOSE);
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(
            PreparedDispatchPlan.dispatch(
                "EVT-2001", "bet-settlements", List.of(losingSettlement)));
    doThrow(new IllegalStateException("RocketMQ unavailable"))
        .when(betSettlementDispatchPort)
        .publish(losingSettlement);

    processEventOutcomeService.process(eventOutcome);

    verify(settlementAuditPort)
        .markFailed("EVT-2001", "BET-2001", "IllegalStateException: RocketMQ unavailable");
    verify(settlementAuditPort, never()).markSent(any(), any());
    verify(settlementFlowMetricsPort).recordPreparedSettlements(1);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    verify(settlementFlowMetricsPort).recordSettlementDispatchCounts(0, 1);
  }

  @Test
  void processHandlesEventsWithoutMatchingBets() {
    when(transactionOperations.execute(any())).thenAnswer(this::executeTransactionCallback);
    EventOutcome eventOutcome = new EventOutcome("EVT-9999", "Team X vs Team Y", "TEAM-X");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(PreparedDispatchPlan.noMatch("EVT-9999", "bet-settlements"));

    processEventOutcomeService.process(eventOutcome);

    verify(betSettlementDispatchPort, never()).publish(any());
    verify(settlementAuditPort, never()).markSent(any(), any());
    verify(settlementAuditPort, never()).markFailed(any(), any(), any());
    verify(settlementFlowMetricsPort).recordPreparedSettlements(0);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.NO_MATCH);
    verify(settlementFlowMetricsPort, never()).recordSettlementDispatchCounts(anyInt(), anyInt());
  }

  @Test
  void processRejectsNullEventOutcome() {
    assertThatThrownBy(() -> processEventOutcomeService.process(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("eventOutcome must not be null");
  }

  private Object executeTransactionCallback(InvocationOnMock invocationOnMock) {
    TransactionCallback<Object> transactionCallback = invocationOnMock.getArgument(0);
    return transactionCallback.doInTransaction(mock(TransactionStatus.class));
  }

  private BetSettlement createSettlement(
      String betId,
      String userId,
      EventOutcome eventOutcome,
      String predictedWinnerId,
      SettlementResult settlementResult) {
    Bet bet =
        new Bet(
            betId,
            userId,
            eventOutcome.eventId(),
            "MARKET-1",
            predictedWinnerId,
            new BigDecimal("25.00"),
            CREATED_AT);
    return new BetSettlement(bet, eventOutcome, settlementResult);
  }
}
