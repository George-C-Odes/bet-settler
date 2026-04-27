package io.github.georgecodes.betsettler.application.port.in;

import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;

/** Replays settlement audit rows that are still pending or previously failed. */
public interface RetrySettlementDispatchUseCase {

  /**
   * Retries settlement dispatches whose audit rows are still retryable.
   *
   * @return replay summary for the retried audit rows
   */
  SettlementDispatchRetrySummary retrySettlements();
}
