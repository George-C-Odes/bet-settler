package io.github.georgecodes.betsettler.infrastructure.persistence.repository;

import io.github.georgecodes.betsettler.infrastructure.persistence.entity.ProcessedEventOutcomeEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for tracked processed event outcomes. */
@SuppressWarnings("unused")
public interface SpringDataProcessedEventOutcomeRepository
    extends JpaRepository<ProcessedEventOutcomeEntity, String> {

  /**
   * Inserts a processed event outcome row.
   *
   * @param eventId unique event identifier
   * @param eventName descriptive event name
   * @param eventWinnerId winning participant identifier
   * @param processedAt processing timestamp
   * @return number of inserted rows
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO processed_event_outcome (event_id, event_name, event_winner_id, processed_at)
          VALUES (:eventId, :eventName, :eventWinnerId, :processedAt)
          """,
      nativeQuery = true)
  int insertProcessedEventOutcome(
      @Param("eventId") String eventId,
      @Param("eventName") String eventName,
      @Param("eventWinnerId") String eventWinnerId,
      @Param("processedAt") Instant processedAt);
}
