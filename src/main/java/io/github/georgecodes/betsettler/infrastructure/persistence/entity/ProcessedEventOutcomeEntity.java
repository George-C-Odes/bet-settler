package io.github.georgecodes.betsettler.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/** JPA entity that records an event outcome that has already been processed. */
@Getter
@SuppressWarnings({"unused"})
@Entity
@Table(name = "processed_event_outcome")
public class ProcessedEventOutcomeEntity {

  /**
   * Primary key value for the processed event identifier. -- GETTER -- Returns the unique event
   * identifier.
   */
  @Id
  @Column(name = "event_id", nullable = false, updatable = false, length = 64)
  private String eventId;

  /**
   * Human-readable name captured for the processed event. -- GETTER -- Returns the descriptive
   * event name.
   */
  @Column(name = "event_name", nullable = false)
  private String eventName;

  /**
   * Winner identifier captured for the processed event. -- GETTER -- Returns the winning
   * participant identifier.
   */
  @Column(name = "event_winner_id", nullable = false, length = 64)
  private String eventWinnerId;

  /**
   * Timestamp recording when the event outcome was processed. -- GETTER -- Returns the processing
   * timestamp.
   */
  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  /** Creates an empty entity instance for JPA. */
  public ProcessedEventOutcomeEntity() {}

  /**
   * Creates a fully populated processed event outcome entity.
   *
   * @param eventId unique event identifier
   * @param eventName descriptive event name
   * @param eventWinnerId winning participant identifier
   * @param processedAt processing timestamp
   */
  public ProcessedEventOutcomeEntity(
      String eventId, String eventName, String eventWinnerId, Instant processedAt) {
    this.eventId = eventId;
    this.eventName = eventName;
    this.eventWinnerId = eventWinnerId;
    this.processedAt = processedAt;
  }
}
