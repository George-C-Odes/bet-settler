package io.github.georgecodes.betsettler.infrastructure.persistence.mapper;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.BetEntity;
import java.util.List;
import java.util.Objects;

/** Maps persisted bet entities to the domain bet model. */
public final class BetEntityMapper {

  /** Prevents instantiation of the mapper utility type. */
  private BetEntityMapper() {}

  /**
   * Converts a bet entity into the domain model.
   *
   * @param entity persisted bet entity
   * @return mapped domain bet
   */
  public static Bet toDomain(BetEntity entity) {
    Objects.requireNonNull(entity, "entity must not be null");
    return new Bet(
        entity.getBetId(),
        entity.getUserId(),
        entity.getEventId(),
        entity.getEventMarketId(),
        entity.getEventWinnerId(),
        entity.getBetAmount(),
        entity.getCreatedAt());
  }

  /**
   * Converts a list of bet entities into domain bets.
   *
   * @param entities persisted bet entities
   * @return mapped domain bets
   */
  public static List<Bet> toDomainList(List<BetEntity> entities) {
    Objects.requireNonNull(entities, "entities must not be null");
    return entities.stream().map(BetEntityMapper::toDomain).toList();
  }
}
