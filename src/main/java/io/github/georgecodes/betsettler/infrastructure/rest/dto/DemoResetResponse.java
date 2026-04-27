package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned by the local or test-only demo reset endpoint.
 *
 * @param status reset operation status
 * @param message human-readable confirmation message
 * @param processedEventOutcomesCleared number of processed-event rows removed
 * @param settlementAuditsCleared number of settlement-audit rows removed
 * @param seededBetsPreserved number of seeded bets preserved by the reset
 */
@Schema(description = "Response returned by the local or test-only demo reset endpoint.")
public record DemoResetResponse(
    String status,
    String message,
    long processedEventOutcomesCleared,
    long settlementAuditsCleared,
    long seededBetsPreserved) {

  /**
   * Creates a validated demo reset response.
   *
   * @param status reset operation status
   * @param message human-readable confirmation message
   * @param processedEventOutcomesCleared number of processed-event rows removed
   * @param settlementAuditsCleared number of settlement-audit rows removed
   * @param seededBetsPreserved number of seeded bets preserved by the reset
   */
  public DemoResetResponse {
    status = requireNonBlank(status, "status");
    message = requireNonBlank(message, "message");
    requireNonNegative(processedEventOutcomesCleared, "processedEventOutcomesCleared");
    requireNonNegative(settlementAuditsCleared, "settlementAuditsCleared");
    requireNonNegative(seededBetsPreserved, "seededBetsPreserved");
  }

  private static void requireNonNegative(long value, String fieldName) {
    if (value < 0L) {
      throw new IllegalArgumentException(fieldName + " must not be negative");
    }
  }
}
