package io.github.georgecodes.betsettler.application.service;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.application.port.in.GetBetsQueryUseCase;
import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default application service for loading seeded or filtered bets. */
@Service
@Transactional(readOnly = true)
public class GetBetsQueryService implements GetBetsQueryUseCase {

  /** Port used to load bets from persistence. */
  private final BetQueryPort betQueryPort;

  /**
   * Creates a new bet query application service.
   *
   * @param betQueryPort port used to load bets from persistence
   */
  public GetBetsQueryService(BetQueryPort betQueryPort) {
    this.betQueryPort = Objects.requireNonNull(betQueryPort, "betQueryPort must not be null");
  }

  @Override
  public List<Bet> getAllBets() {
    return betQueryPort.getAllBets();
  }

  @Override
  public List<Bet> getBetsByEventId(String eventId) {
    return betQueryPort.getBetsByEventId(requireNonBlank(eventId, "eventId"));
  }
}
