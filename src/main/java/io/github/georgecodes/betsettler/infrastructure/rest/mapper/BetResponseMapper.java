package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.BetResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps domain bets into REST response payloads for bet-query endpoints. */
@Component
public class BetResponseMapper {

  /** Creates a new bet-response mapper. */
  public BetResponseMapper() {}

  /**
   * Converts a domain bet into a REST response payload.
   *
   * @param bet domain bet to convert
   * @return REST response payload for the supplied bet
   */
  public BetResponse toBetResponse(Bet bet) {
    Objects.requireNonNull(bet, "bet must not be null");
    return new BetResponse(
        bet.betId(),
        bet.userId(),
        bet.eventId(),
        bet.eventMarketId(),
        bet.eventWinnerId(),
        bet.betAmount(),
        bet.createdAt());
  }

  /**
   * Converts a list of domain bets into REST response payloads.
   *
   * @param bets domain bets to convert
   * @return REST response payloads for the supplied bets
   */
  public List<BetResponse> toBetResponses(List<Bet> bets) {
    Objects.requireNonNull(bets, "bets must not be null");
    return bets.stream().map(this::toBetResponse).toList();
  }
}
