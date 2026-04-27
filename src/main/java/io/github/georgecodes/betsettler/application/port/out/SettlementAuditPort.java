package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import java.util.List;

/** Persists audit information about settlement publication attempts. */
public interface SettlementAuditPort {

  /**
   * Saves multiple pending settlement publication audit entries in one persistence operation.
   *
   * @param pendingSettlements pending settlement audit descriptors to persist
   */
  void savePendingSettlements(List<PendingSettlementAudit> pendingSettlements);

  /**
   * Returns settlement audit rows that are eligible for replay because they are still pending or
   * failed during an earlier dispatch attempt.
   *
   * @return retryable settlement audit descriptors ordered for replay
   */
  List<RetryableSettlementAudit> findRetryableSettlements();

  /**
   * Marks a settlement publication as sent.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   */
  void markSent(String eventId, String betId);

  /**
   * Marks a settlement publication as failed.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   * @param failureReason reason for the failed publication
   */
  void markFailed(String eventId, String betId, String failureReason);
}
