package io.github.georgecodes.betsettler.application.port.in;

import io.github.georgecodes.betsettler.domain.model.Bet;
import java.util.List;

/** Queries the bets that are available to the application. */
public interface GetBetsQueryUseCase {

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
