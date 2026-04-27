package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import java.math.BigDecimal;

/**
 * Shared settlement payload representing a bet settlement decision.
 *
 * @param betId unique bet identifier
 * @param userId unique user identifier
 * @param eventId related event identifier
 * @param eventName related event name
 * @param eventMarketId related event market identifier
 * @param predictedWinnerId winner identifier selected by the bet
 * @param actualWinnerId winning identifier from the processed event outcome
 * @param betAmount stake amount placed on the bet
 * @param settlementResult resulting settlement status
 */
@SuppressWarnings("unused")
public record BetSettlementMessage(
    String betId,
    String userId,
    String eventId,
    String eventName,
    String eventMarketId,
    String predictedWinnerId,
    String actualWinnerId,
    BigDecimal betAmount,
    String settlementResult) {

  /**
   * Creates a validated bet settlement message.
   *
   * @param betId unique bet identifier
   * @param userId unique user identifier
   * @param eventId related event identifier
   * @param eventName related event name
   * @param eventMarketId related event market identifier
   * @param predictedWinnerId winner identifier selected by the bet
   * @param actualWinnerId winning identifier from the processed event outcome
   * @param betAmount stake amount placed on the bet
   * @param settlementResult resulting settlement status
   */
  public BetSettlementMessage {
    betId = ValidationSupport.requireNonBlank(betId, "betId");
    userId = ValidationSupport.requireNonBlank(userId, "userId");
    eventId = ValidationSupport.requireNonBlank(eventId, "eventId");
    eventName = ValidationSupport.requireNonBlank(eventName, "eventName");
    eventMarketId = ValidationSupport.requireNonBlank(eventMarketId, "eventMarketId");
    predictedWinnerId = ValidationSupport.requireNonBlank(predictedWinnerId, "predictedWinnerId");
    actualWinnerId = ValidationSupport.requireNonBlank(actualWinnerId, "actualWinnerId");
    ValidationSupport.requirePositive(betAmount, "betAmount");
    settlementResult = ValidationSupport.requireNonBlank(settlementResult, "settlementResult");
  }
}
