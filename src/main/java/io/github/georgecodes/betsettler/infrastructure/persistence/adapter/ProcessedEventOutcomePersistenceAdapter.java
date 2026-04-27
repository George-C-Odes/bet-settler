package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import io.github.georgecodes.betsettler.application.port.out.ProcessedEventOutcomePort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.persistence.mapper.ProcessedEventOutcomeEntityMapper;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Spring Data JPA implementation of the processed event outcome port. */
@Component
@Transactional
public class ProcessedEventOutcomePersistenceAdapter implements ProcessedEventOutcomePort {

  /** Spring Data repository used to persist processed event outcomes. */
  private final SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  /** Shared UTC clock used to timestamp processed event outcome writes. */
  private final Clock persistenceClock;

  /**
   * Creates a new processed event outcome persistence adapter.
   *
   * @param processedEventOutcomeRepository Spring Data repository used to track processed events
   * @param persistenceClock shared clock used to timestamp writes
   */
  public ProcessedEventOutcomePersistenceAdapter(
      SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository,
      Clock persistenceClock) {
    this.processedEventOutcomeRepository =
        Objects.requireNonNull(
            processedEventOutcomeRepository, "processedEventOutcomeRepository must not be null");
    this.persistenceClock =
        Objects.requireNonNull(persistenceClock, "persistenceClock must not be null");
  }

  @Override
  public boolean recordProcessedIfAbsent(EventOutcome eventOutcome) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    Instant processedAt = Instant.now(persistenceClock);
    var processedEventOutcomeEntity =
        ProcessedEventOutcomeEntityMapper.toEntity(eventOutcome, processedAt);
    try {
      return processedEventOutcomeRepository.insertProcessedEventOutcome(
              processedEventOutcomeEntity.getEventId(),
              processedEventOutcomeEntity.getEventName(),
              processedEventOutcomeEntity.getEventWinnerId(),
              processedEventOutcomeEntity.getProcessedAt())
          == 1;
    } catch (DataIntegrityViolationException ignored) {
      return false;
    }
  }
}
