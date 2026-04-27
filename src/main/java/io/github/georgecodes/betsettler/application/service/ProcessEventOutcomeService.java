package io.github.georgecodes.betsettler.application.service;

import io.github.georgecodes.betsettler.application.model.dispatch.PreparedDispatchPlan;
import io.github.georgecodes.betsettler.application.port.in.ProcessEventOutcomeUseCase;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.github.georgecodes.betsettler.application.support.SettlementFailureReasonFormatter;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Orchestrates settlement preparation and dispatch for consumed event outcomes.
 *
 * <p>The implementation intentionally uses a two-step flow: a transactional preparation stage that
 * records dedup and pending audit state, followed by an out-of-transaction publication stage.
 */
@Slf4j
@Service
public class ProcessEventOutcomeService implements ProcessEventOutcomeUseCase {

  /** Transaction abstraction used for the preparation stage. */
  private final TransactionOperations transactionOperations;

  /** Collaborator used to prepare deduplicated settlement dispatch plans inside the transaction. */
  private final SettlementDispatchPreparationService settlementDispatchPreparationService;

  /** Port used to persist and update settlement audit rows. */
  private final SettlementAuditPort settlementAuditPort;

  /** Port used to publish settlement messages. */
  private final BetSettlementDispatchPort betSettlementDispatchPort;

  /** Formatter used to convert publication failures into bounded audit strings. */
  private final SettlementFailureReasonFormatter settlementFailureReasonFormatter;

  /** Metrics port used to expose settlement-processing activity. */
  private final SettlementFlowMetricsPort settlementFlowMetricsPort;

  /**
   * Creates a new process event outcome application service.
   *
   * @param transactionOperations transaction abstraction used for the preparation stage
   * @param settlementDispatchPreparationService collaborator used to prepare deduplicated
   *     settlement dispatch plans
   * @param settlementAuditPort port used to persist and update settlement audits
   * @param betSettlementDispatchPort port used to publish settlement messages
   * @param settlementFailureReasonFormatter formatter used to create bounded failure reasons for
   *     settlement audit updates
   * @param settlementFlowMetricsPort metrics port used to expose settlement-processing activity
   */
  public ProcessEventOutcomeService(
      TransactionOperations transactionOperations,
      SettlementDispatchPreparationService settlementDispatchPreparationService,
      SettlementAuditPort settlementAuditPort,
      BetSettlementDispatchPort betSettlementDispatchPort,
      SettlementFailureReasonFormatter settlementFailureReasonFormatter,
      SettlementFlowMetricsPort settlementFlowMetricsPort) {
    this.transactionOperations =
        Objects.requireNonNull(transactionOperations, "transactionOperations must not be null");
    this.settlementDispatchPreparationService =
        Objects.requireNonNull(
            settlementDispatchPreparationService,
            "settlementDispatchPreparationService must not be null");
    this.settlementAuditPort =
        Objects.requireNonNull(settlementAuditPort, "settlementAuditPort must not be null");
    this.betSettlementDispatchPort =
        Objects.requireNonNull(
            betSettlementDispatchPort, "betSettlementDispatchPort must not be null");
    this.settlementFailureReasonFormatter =
        Objects.requireNonNull(
            settlementFailureReasonFormatter, "settlementFailureReasonFormatter must not be null");
    this.settlementFlowMetricsPort =
        Objects.requireNonNull(
            settlementFlowMetricsPort, "settlementFlowMetricsPort must not be null");
  }

  @Override
  public void process(EventOutcome eventOutcome) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    log.info(
        "Processing consumed event outcome eventId={} eventName={} eventWinnerId={}",
        eventOutcome.eventId(),
        eventOutcome.eventName(),
        eventOutcome.eventWinnerId());
    PreparedDispatchPlan preparedDispatchPlan =
        Objects.requireNonNull(
            transactionOperations.execute(
                ignoredStatus -> settlementDispatchPreparationService.prepare(eventOutcome)),
            "preparedDispatchPlan must not be null");

    if (preparedDispatchPlan.isDuplicate()) {
      settlementFlowMetricsPort.recordProcessedEventOutcome(
          SettlementFlowMetricsPort.EventOutcomeProcessingResult.DUPLICATE);
      log.info(
          "Skipping duplicate event outcome processing for eventId={} processingResult=duplicate",
          preparedDispatchPlan.eventId());
      return;
    }

    settlementFlowMetricsPort.recordPreparedSettlements(preparedDispatchPlan.settlements().size());
    if (!preparedDispatchPlan.requiresDispatch()) {
      if (!preparedDispatchPlan.hasDestinationTopic()) {
        throw new IllegalStateException(
            "destinationTopic is unavailable for status=" + preparedDispatchPlan.status());
      }
      String destinationTopic = preparedDispatchPlan.requiredDestinationTopic();
      settlementFlowMetricsPort.recordProcessedEventOutcome(
          SettlementFlowMetricsPort.EventOutcomeProcessingResult.NO_MATCH);
      log.info(
          "Processed event outcome with no matching bets eventId={} destinationTopic={} processingResult=no-match",
          preparedDispatchPlan.eventId(),
          destinationTopic);
      return;
    }

    publishPreparedDispatchPlan(preparedDispatchPlan);
  }

  private void publishPreparedDispatchPlan(PreparedDispatchPlan preparedDispatchPlan) {
    String destinationTopic = preparedDispatchPlan.requiredDestinationTopic();
    int sentCount = 0;
    int failedCount = 0;

    for (BetSettlement settlement : preparedDispatchPlan.settlements()) {
      try {
        betSettlementDispatchPort.publish(settlement);
        settlementAuditPort.markSent(settlement.eventOutcome().eventId(), settlement.bet().betId());
        sentCount++;
      } catch (RuntimeException exception) {
        failedCount++;
        markSettlementFailure(settlement, exception);
      }
    }

    log.info(
        "Processed event outcome eventId={} destinationTopic={} settlementCount={} sentCount={} failedCount={}",
        preparedDispatchPlan.eventId(),
        destinationTopic,
        preparedDispatchPlan.settlements().size(),
        sentCount,
        failedCount);
    settlementFlowMetricsPort.recordProcessedEventOutcome(
        SettlementFlowMetricsPort.EventOutcomeProcessingResult.DISPATCHED);
    settlementFlowMetricsPort.recordSettlementDispatchCounts(sentCount, failedCount);
  }

  private void markSettlementFailure(BetSettlement settlement, RuntimeException exception) {
    String failureReason = settlementFailureReasonFormatter.format(exception);
    try {
      settlementAuditPort.markFailed(
          settlement.eventOutcome().eventId(), settlement.bet().betId(), failureReason);
    } catch (RuntimeException auditException) {
      log.error(
          "Failed to update settlement audit after publication failure for eventId={} betId={} userId={}",
          settlement.eventOutcome().eventId(),
          settlement.bet().betId(),
          settlement.bet().userId(),
          auditException);
    }

    log.error(
        "Failed to publish settlement for eventId={} betId={} userId={} reason={}",
        settlement.eventOutcome().eventId(),
        settlement.bet().betId(),
        settlement.bet().userId(),
        failureReason,
        exception);
  }
}
