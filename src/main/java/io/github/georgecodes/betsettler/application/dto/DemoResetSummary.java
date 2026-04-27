package io.github.georgecodes.betsettler.application.dto;

/**
 * Summary returned after clearing transient demo-state tables.
 *
 * @param processedEventOutcomesCleared number of processed-event rows removed
 * @param settlementAuditsCleared number of settlement-audit rows removed
 * @param seededBetsPreserved number of seeded bet rows left intact
 */
public record DemoResetSummary(
    long processedEventOutcomesCleared, long settlementAuditsCleared, long seededBetsPreserved) {

  /**
   * Creates a validated demo reset summary.
   *
   * @param processedEventOutcomesCleared number of processed-event rows removed
   * @param settlementAuditsCleared number of settlement-audit rows removed
   * @param seededBetsPreserved number of seeded bet rows left intact
   */
  public DemoResetSummary {
    requireNonNegative(processedEventOutcomesCleared, "processedEventOutcomesCleared");
    requireNonNegative(settlementAuditsCleared, "settlementAuditsCleared");
    requireNonNegative(seededBetsPreserved, "seededBetsPreserved");
  }

  /**
   * Returns the total number of transient rows cleared by the reset operation.
   *
   * @return total transient rows removed
   */
  public long totalClearedRows() {
    return processedEventOutcomesCleared + settlementAuditsCleared;
  }

  private static void requireNonNegative(long value, String fieldName) {
    if (value < 0L) {
      throw new IllegalArgumentException(fieldName + " must not be negative");
    }
  }
}
