package io.github.georgecodes.betsettler.domain.service;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import java.util.Objects;

/** Applies the domain rule that determines whether a bet wins or loses. */
public final class BetSettlementDecider {

  /** Creates a stateless settlement decider. */
  public BetSettlementDecider() {}

  /**
   * Determines the settlement result for a bet and event outcome.
   *
   * @param bet bet to evaluate
   * @param eventOutcome event outcome used for the evaluation
   * @return the resulting settlement status
   */
  public SettlementResult decide(Bet bet, EventOutcome eventOutcome) {
    validateInput(bet, eventOutcome);
    return bet.eventWinnerId().equals(eventOutcome.eventWinnerId())
        ? SettlementResult.WIN
        : SettlementResult.LOSE;
  }

  /**
   * Creates a settlement aggregate for a bet and event outcome.
   *
   * @param bet bet to evaluate
   * @param eventOutcome event outcome used for the evaluation
   * @return the completed settlement decision
   */
  public BetSettlement settle(Bet bet, EventOutcome eventOutcome) {
    return new BetSettlement(bet, eventOutcome, decide(bet, eventOutcome));
  }

  private void validateInput(Bet bet, EventOutcome eventOutcome) {
    Objects.requireNonNull(bet, "bet must not be null");
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    if (!bet.eventId().equals(eventOutcome.eventId())) {
      throw new IllegalArgumentException("bet.eventId and eventOutcome.eventId must match");
    }
  }
}
