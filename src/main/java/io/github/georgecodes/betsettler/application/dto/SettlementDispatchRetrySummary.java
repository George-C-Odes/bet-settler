package io.github.georgecodes.betsettler.application.dto;

/**
 * Summary returned after replaying pending or failed settlement audit rows.
 *
 * @param retriedAuditCount number of retryable audit rows that were considered
 * @param sentCount number of rows successfully replayed
 * @param failedCount number of rows that still failed during replay
 */
public record SettlementDispatchRetrySummary(
    long retriedAuditCount, long sentCount, long failedCount) {

  /**
   * Creates a validated settlement-dispatch retry summary.
   *
   * @param retriedAuditCount number of retryable audit rows that were considered
   * @param sentCount number of rows successfully replayed
   * @param failedCount number of rows that still failed during replay
   */
  public SettlementDispatchRetrySummary {
    requireNonNegative(retriedAuditCount, "retriedAuditCount");
    requireNonNegative(sentCount, "sentCount");
    requireNonNegative(failedCount, "failedCount");
    if (sentCount + failedCount > retriedAuditCount) {
      throw new IllegalArgumentException(
          "sentCount plus failedCount must not exceed retriedAuditCount");
    }
  }

  /**
   * Returns {@code true} when at least one settlement replay still failed.
   *
   * @return {@code true} when one or more replays failed
   */
  public boolean hasFailures() {
    return failedCount > 0L;
  }

  private static void requireNonNegative(long value, String fieldName) {
    if (value < 0L) {
      throw new IllegalArgumentException(fieldName + " must not be negative");
    }
  }
}
