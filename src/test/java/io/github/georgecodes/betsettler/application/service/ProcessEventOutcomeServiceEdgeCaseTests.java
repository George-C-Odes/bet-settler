package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import java.util.concurrent.atomic.AtomicReference;
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
class ProcessEventOutcomeServiceEdgeCaseTests {

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
    lenient()
        .when(transactionOperations.execute(any()))
        .thenAnswer(this::executeTransactionCallback);
  }

  @Test
  void processTreatsAtomicProcessedInsertRaceAsDuplicate() {
    EventOutcome eventOutcome = new EventOutcome("EVT-4001", "Team G vs Team H", "TEAM-G");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(PreparedDispatchPlan.duplicate("EVT-4001"));

    processEventOutcomeService.process(eventOutcome);

    verify(settlementDispatchPreparationService).prepare(eventOutcome);
    verify(betSettlementDispatchPort, never()).publish(any());
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DUPLICATE);
  }

  @Test
  void processUsesExceptionTypeOnlyWhenPublicationFailureMessageIsBlank() {
    EventOutcome eventOutcome = new EventOutcome("EVT-5001", "Team I vs Team J", "TEAM-I");
    BetSettlement settlement = createSettlement("BET-5001", "USER-5", eventOutcome, "TEAM-I");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(
            PreparedDispatchPlan.dispatch("EVT-5001", "bet-settlements", List.of(settlement)));
    doThrow(new IllegalArgumentException("   "))
        .when(betSettlementDispatchPort)
        .publish(settlement);

    processEventOutcomeService.process(eventOutcome);

    verify(settlementAuditPort)
        .markFailed(eq("EVT-5001"), eq("BET-5001"), eq("IllegalArgumentException"));
    verify(settlementFlowMetricsPort).recordPreparedSettlements(1);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    verify(settlementFlowMetricsPort).recordSettlementDispatchCounts(0, 1);
  }

  @Test
  void processUsesExceptionTypeOnlyWhenPublicationFailureMessageIsNull() {
    EventOutcome eventOutcome = new EventOutcome("EVT-5002", "Team I vs Team J", "TEAM-I");
    BetSettlement settlement = createSettlement("BET-5002", "USER-5", eventOutcome, "TEAM-I");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(
            PreparedDispatchPlan.dispatch("EVT-5002", "bet-settlements", List.of(settlement)));
    doThrow(new IllegalArgumentException((String) null))
        .when(betSettlementDispatchPort)
        .publish(settlement);

    processEventOutcomeService.process(eventOutcome);

    verify(settlementAuditPort)
        .markFailed(eq("EVT-5002"), eq("BET-5002"), eq("IllegalArgumentException"));
    verify(settlementFlowMetricsPort).recordPreparedSettlements(1);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    verify(settlementFlowMetricsPort).recordSettlementDispatchCounts(0, 1);
  }

  @Test
  void processTruncatesFailureReasonAndContinuesWhenAuditUpdateFails() {
    EventOutcome eventOutcome = new EventOutcome("EVT-6001", "Team K vs Team L", "TEAM-K");
    BetSettlement settlement = createSettlement("BET-6001", "USER-6", eventOutcome, "TEAM-K");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenReturn(
            PreparedDispatchPlan.dispatch("EVT-6001", "bet-settlements", List.of(settlement)));

    String veryLongMessage =
        "x".repeat(SettlementFailureReasonFormatter.MAX_FAILURE_REASON_LENGTH + 64);
    doThrow(new IllegalStateException(veryLongMessage))
        .when(betSettlementDispatchPort)
        .publish(settlement);
    AtomicReference<String> recordedFailureReason = new AtomicReference<>();
    doAnswer(
            invocation -> {
              recordedFailureReason.set(invocation.getArgument(2, String.class));
              throw new IllegalStateException("Audit repository unavailable");
            })
        .when(settlementAuditPort)
        .markFailed(eq("EVT-6001"), eq("BET-6001"), any());

    processEventOutcomeService.process(eventOutcome);

    assertThat(recordedFailureReason.get())
        .startsWith("IllegalStateException: ")
        .hasSize(SettlementFailureReasonFormatter.MAX_FAILURE_REASON_LENGTH);
    verify(settlementFlowMetricsPort).recordPreparedSettlements(1);
    verify(settlementFlowMetricsPort)
        .recordProcessedEventOutcome(
            SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    verify(settlementFlowMetricsPort).recordSettlementDispatchCounts(0, 1);
  }

  @Test
  void processRejectsBlankDestinationTopic() {
    EventOutcome eventOutcome = new EventOutcome("EVT-7001", "Team M vs Team N", "TEAM-M");
    when(settlementDispatchPreparationService.prepare(eventOutcome))
        .thenThrow(new IllegalArgumentException("destinationTopic must not be blank"));

    assertThatThrownBy(() -> processEventOutcomeService.process(eventOutcome))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("destinationTopic must not be blank");

    verify(settlementFlowMetricsPort, never()).recordPreparedSettlements(anyInt());
  }

  private BetSettlement createSettlement(
      String betId, String userId, EventOutcome eventOutcome, String predictedWinnerId) {
    Bet bet =
        new Bet(
            betId,
            userId,
            eventOutcome.eventId(),
            "MARKET-1",
            predictedWinnerId,
            new BigDecimal("25.00"),
            CREATED_AT);
    return new BetSettlement(bet, eventOutcome, SettlementResult.WIN);
  }

  private Object executeTransactionCallback(InvocationOnMock invocationOnMock) {
    TransactionCallback<Object> transactionCallback = invocationOnMock.getArgument(0);
    return transactionCallback.doInTransaction(mock(TransactionStatus.class));
  }
}
