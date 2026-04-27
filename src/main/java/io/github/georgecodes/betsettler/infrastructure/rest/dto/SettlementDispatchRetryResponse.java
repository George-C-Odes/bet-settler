package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after replaying retryable settlement audit rows through the configured
 * publisher.
 *
 * @param status transport-level status for the retry trigger
 * @param message human-readable retry summary message
 * @param retriedAuditCount number of retryable audit rows considered
 * @param sentCount number of settlement dispatches successfully replayed
 * @param failedCount number of settlement dispatches that still failed during replay
 */
@Schema(description = "Response returned after replaying retryable settlement audit rows.")
public record SettlementDispatchRetryResponse(
    String status, String message, long retriedAuditCount, long sentCount, long failedCount) {

  /**
   * Creates a validated settlement-dispatch retry response.
   *
   * @param status transport-level status for the retry trigger
   * @param message human-readable retry summary message
   * @param retriedAuditCount number of retryable audit rows considered
   * @param sentCount number of settlement dispatches successfully replayed
   * @param failedCount number of settlement dispatches that still failed during replay
   */
  public SettlementDispatchRetryResponse {
    status = requireNonBlank(status, "status");
    message = requireNonBlank(message, "message");
    requireNonNegative(retriedAuditCount, "retriedAuditCount");
    requireNonNegative(sentCount, "sentCount");
    requireNonNegative(failedCount, "failedCount");
    if (sentCount + failedCount > retriedAuditCount) {
      throw new IllegalArgumentException(
          "sentCount plus failedCount must not exceed retriedAuditCount");
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }

  private static void requireNonNegative(long value, String fieldName) {
    if (value < 0L) {
      throw new IllegalArgumentException(fieldName + " must not be negative");
    }
  }
}
