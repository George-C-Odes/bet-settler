package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Read-only response payload that exposes a seeded or matched bet.
 *
 * @param betId unique bet identifier
 * @param userId unique user identifier
 * @param eventId related event identifier
 * @param eventMarketId related event market identifier
 * @param eventWinnerId predicted winner identifier captured by the bet
 * @param betAmount stake amount placed on the bet
 * @param createdAt bet creation timestamp
 */
@Schema(description = "Read-only response payload that exposes a seeded or matched bet.")
public record BetResponse(
    String betId,
    String userId,
    String eventId,
    String eventMarketId,
    String eventWinnerId,
    BigDecimal betAmount,
    Instant createdAt) {

  /**
   * Creates a validated bet response.
   *
   * @param betId unique bet identifier
   * @param userId unique user identifier
   * @param eventId related event identifier
   * @param eventMarketId related event market identifier
   * @param eventWinnerId predicted winner identifier captured by the bet
   * @param betAmount stake amount placed on the bet
   * @param createdAt bet creation timestamp
   */
  public BetResponse {
    betId = requireNonBlank(betId, "betId");
    userId = requireNonBlank(userId, "userId");
    eventId = requireNonBlank(eventId, "eventId");
    eventMarketId = requireNonBlank(eventMarketId, "eventMarketId");
    eventWinnerId = requireNonBlank(eventWinnerId, "eventWinnerId");
    Objects.requireNonNull(betAmount, "betAmount must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
