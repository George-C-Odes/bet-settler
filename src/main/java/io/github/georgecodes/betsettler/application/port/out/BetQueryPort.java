package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.domain.model.Bet;
import java.util.List;

/** Loads bets from a backing store. */
public interface BetQueryPort {

  /**
   * Returns every available bet.
   *
   * @return all bets
   */
  List<Bet> getAllBets();

  /**
   * Returns only the bets for a specific event.
   *
   * @param eventId event identifier filter
   * @return matching bets
   */
  List<Bet> getBetsByEventId(String eventId);
}
