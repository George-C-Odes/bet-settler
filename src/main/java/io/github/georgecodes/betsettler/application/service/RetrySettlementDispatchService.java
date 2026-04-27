package io.github.georgecodes.betsettler.application.service;

import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import io.github.georgecodes.betsettler.application.port.in.RetrySettlementDispatchUseCase;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.application.support.SettlementFailureReasonFormatter;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Replays pending or failed settlement audit rows through the configured settlement replay port.
 */
@Slf4j
@Service
public class RetrySettlementDispatchService implements RetrySettlementDispatchUseCase {

  /** Port used to query and update settlement audit rows. */
  private final SettlementAuditPort settlementAuditPort;

  /** Port used to replay settlement payloads to the configured publisher. */
  private final BetSettlementReplayPort betSettlementReplayPort;

  /** Formatter used to convert replay failures into bounded audit strings. */
  private final SettlementFailureReasonFormatter settlementFailureReasonFormatter;

  /**
   * Creates a new settlement-dispatch retry service.
   *
   * @param settlementAuditPort port used to query and update settlement audit rows
   * @param betSettlementReplayPort port used to replay settlement payloads
   * @param settlementFailureReasonFormatter formatter used to create bounded failure reasons for
   *     settlement audit updates
   */
  public RetrySettlementDispatchService(
      SettlementAuditPort settlementAuditPort,
      BetSettlementReplayPort betSettlementReplayPort,
      SettlementFailureReasonFormatter settlementFailureReasonFormatter) {
    this.settlementAuditPort =
        Objects.requireNonNull(settlementAuditPort, "settlementAuditPort must not be null");
    this.betSettlementReplayPort =
        Objects.requireNonNull(betSettlementReplayPort, "betSettlementReplayPort must not be null");
    this.settlementFailureReasonFormatter =
        Objects.requireNonNull(
            settlementFailureReasonFormatter, "settlementFailureReasonFormatter must not be null");
  }

  @Override
  public SettlementDispatchRetrySummary retrySettlements() {
    List<RetryableSettlementAudit> retryableSettlements =
        settlementAuditPort.findRetryableSettlements();
    long sentCount = 0L;
    long failedCount = 0L;

    for (RetryableSettlementAudit retryableSettlement : retryableSettlements) {
      try {
        betSettlementReplayPort.replayPayloadSnapshot(
            retryableSettlement.destinationTopic(), retryableSettlement.payloadSnapshot());
        settlementAuditPort.markSent(retryableSettlement.eventId(), retryableSettlement.betId());
        sentCount++;
      } catch (RuntimeException exception) {
        failedCount++;
        String failureReason = settlementFailureReasonFormatter.format(exception);
        settlementAuditPort.markFailed(
            retryableSettlement.eventId(), retryableSettlement.betId(), failureReason);
        log.error(
            "Retry of settlement dispatch failed for eventId={} betId={} userId={} status={} reason={}",
            retryableSettlement.eventId(),
            retryableSettlement.betId(),
            retryableSettlement.userId(),
            retryableSettlement.publishStatus(),
            failureReason,
            exception);
      }
    }

    log.info(
        "Retried settlement dispatches retriedAuditCount={} sentCount={} failedCount={}",
        retryableSettlements.size(),
        sentCount,
        failedCount);
    return new SettlementDispatchRetrySummary(retryableSettlements.size(), sentCount, failedCount);
  }
}
