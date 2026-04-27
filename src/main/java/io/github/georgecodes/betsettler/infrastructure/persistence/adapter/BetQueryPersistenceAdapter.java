package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.infrastructure.persistence.mapper.BetEntityMapper;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataBetRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Spring Data JPA implementation of the bet query port. */
@Component
@Transactional(readOnly = true)
public class BetQueryPersistenceAdapter implements BetQueryPort {

  /** Spring Data repository used to load persisted bets. */
  private final SpringDataBetRepository betRepository;

  /**
   * Creates a new bet query persistence adapter.
   *
   * @param betRepository Spring Data repository used to load bets
   */
  public BetQueryPersistenceAdapter(SpringDataBetRepository betRepository) {
    this.betRepository = Objects.requireNonNull(betRepository, "betRepository must not be null");
  }

  @Override
  public List<Bet> getAllBets() {
    return BetEntityMapper.toDomainList(betRepository.findAllByOrderByBetIdAsc());
  }

  @Override
  public List<Bet> getBetsByEventId(String eventId) {
    return BetEntityMapper.toDomainList(
        betRepository.findByEventIdOrderByBetIdAsc(requireNonBlank(eventId, "eventId")));
  }
}
