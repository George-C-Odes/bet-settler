package io.github.georgecodes.betsettler.application.model.audit;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;

/**
 * Retry descriptor for a settlement audit row that can be replayed through the settlement
 * publisher.
 *
 * @param eventId related event identifier
 * @param betId related bet identifier
 * @param userId related user identifier
 * @param publishStatus current persisted publish status
 * @param destinationTopic original destination topic or channel name
 * @param payloadSnapshot serialized settlement payload to replay
 */
public record RetryableSettlementAudit(
    String eventId,
    String betId,
    String userId,
    String publishStatus,
    String destinationTopic,
    String payloadSnapshot) {

  /**
   * Creates a validated retryable settlement audit descriptor.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   * @param userId related user identifier
   * @param publishStatus current persisted publish status
   * @param destinationTopic original destination topic or channel name
   * @param payloadSnapshot serialized settlement payload to replay
   */
  public RetryableSettlementAudit {
    eventId = normalizeRequiredValue(eventId, "eventId");
    betId = normalizeRequiredValue(betId, "betId");
    userId = normalizeRequiredValue(userId, "userId");
    publishStatus = normalizeRequiredValue(publishStatus, "publishStatus");
    destinationTopic = normalizeRequiredValue(destinationTopic, "destinationTopic");
    payloadSnapshot = normalizeRequiredValue(payloadSnapshot, "payloadSnapshot");
  }

  private static String normalizeRequiredValue(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
