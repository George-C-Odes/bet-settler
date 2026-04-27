package io.github.georgecodes.betsettler.infrastructure.persistence.mapper;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.ProcessedEventOutcomeEntity;
import java.time.Instant;
import java.util.Objects;

/** Maps domain event outcomes into processed event outcome entities. */
public final class ProcessedEventOutcomeEntityMapper {

  /** Prevents instantiation of the mapper utility type. */
  private ProcessedEventOutcomeEntityMapper() {}

  /**
   * Converts a processed domain event outcome into a persistence entity.
   *
   * @param eventOutcome processed domain event outcome
   * @param processedAt processing timestamp
   * @return mapped persistence entity
   */
  public static ProcessedEventOutcomeEntity toEntity(
      EventOutcome eventOutcome, Instant processedAt) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    Objects.requireNonNull(processedAt, "processedAt must not be null");
    return new ProcessedEventOutcomeEntity(
        eventOutcome.eventId(),
        eventOutcome.eventName(),
        eventOutcome.eventWinnerId(),
        processedAt);
  }
}
