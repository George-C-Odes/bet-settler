package io.github.georgecodes.betsettler.domain.model;

import java.util.Objects;

/**
 * Represents a settlement decision for a specific bet and event outcome.
 *
 * @param bet bet that has been evaluated
 * @param eventOutcome event outcome used for the evaluation
 * @param settlementResult resulting settlement status for the bet
 */
public record BetSettlement(Bet bet, EventOutcome eventOutcome, SettlementResult settlementResult) {

  /**
   * Creates a validated settlement decision.
   *
   * @param bet bet that has been evaluated
   * @param eventOutcome event outcome used for the evaluation
   * @param settlementResult resulting settlement status for the bet
   */
  public BetSettlement {
    Objects.requireNonNull(bet, "bet must not be null");
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    Objects.requireNonNull(settlementResult, "settlementResult must not be null");
    if (!bet.eventId().equals(eventOutcome.eventId())) {
      throw new IllegalArgumentException("bet.eventId and eventOutcome.eventId must match");
    }
  }
}
