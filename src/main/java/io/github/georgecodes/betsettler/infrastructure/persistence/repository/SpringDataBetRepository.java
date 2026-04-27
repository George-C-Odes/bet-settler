package io.github.georgecodes.betsettler.infrastructure.persistence.repository;

import io.github.georgecodes.betsettler.infrastructure.persistence.entity.BetEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for reading persisted bets. */
@SuppressWarnings("unused")
public interface SpringDataBetRepository extends JpaRepository<BetEntity, String> {

  /**
   * Returns every persisted bet ordered by identifier.
   *
   * @return all persisted bets ordered by identifier
   */
  List<BetEntity> findAllByOrderByBetIdAsc();

  /**
   * Returns bets for a specific event ordered by identifier.
   *
   * @param eventId related event identifier
   * @return matching bets ordered by identifier
   */
  List<BetEntity> findByEventIdOrderByBetIdAsc(String eventId);
}
