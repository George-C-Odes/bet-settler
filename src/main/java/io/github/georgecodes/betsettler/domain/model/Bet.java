package io.github.georgecodes.betsettler.domain.model;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a bet that can be evaluated against an event outcome.
 *
 * @param betId unique bet identifier
 * @param userId unique user identifier
 * @param eventId event identifier used for settlement matching
 * @param eventMarketId market identifier associated with the bet
 * @param eventWinnerId predicted event winner identifier captured by the bet
 * @param betAmount stake amount placed on the bet
 * @param createdAt creation timestamp of the bet
 */
public record Bet(
    String betId,
    String userId,
    String eventId,
    String eventMarketId,
    String eventWinnerId,
    BigDecimal betAmount,
    Instant createdAt) {

  /**
   * Creates a validated bet.
   *
   * @param betId unique bet identifier
   * @param userId unique user identifier
   * @param eventId event identifier used for settlement matching
   * @param eventMarketId market identifier associated with the bet
   * @param eventWinnerId predicted event winner identifier captured by the bet
   * @param betAmount stake amount placed on the bet
   * @param createdAt creation timestamp of the bet
   */
  public Bet {
    betId = ValidationSupport.requireNonBlank(betId, "betId");
    userId = ValidationSupport.requireNonBlank(userId, "userId");
    eventId = ValidationSupport.requireNonBlank(eventId, "eventId");
    eventMarketId = ValidationSupport.requireNonBlank(eventMarketId, "eventMarketId");
    eventWinnerId = ValidationSupport.requireNonBlank(eventWinnerId, "eventWinnerId");
    ValidationSupport.requirePositive(betAmount, "betAmount");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
