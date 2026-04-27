package io.github.georgecodes.betsettler.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

/** JPA entity that stores a bet row in the in-memory database. */
@Getter
@SuppressWarnings({"unused"})
@Entity
@Table(name = "bet")
public class BetEntity {

  /**
   * Primary key value for the persisted bet row. -- GETTER -- Returns the unique bet identifier.
   */
  @Id
  @Column(name = "bet_id", nullable = false, updatable = false, length = 64)
  private String betId;

  /**
   * User identifier associated with the persisted bet. -- GETTER -- Returns the unique user
   * identifier.
   */
  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  /**
   * Event identifier used to match the bet during settlement processing. -- GETTER -- Returns the
   * related event identifier.
   */
  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  /**
   * Market identifier associated with the persisted bet. -- GETTER -- Returns the related event
   * market identifier.
   */
  @Column(name = "event_market_id", nullable = false, length = 64)
  private String eventMarketId;

  /**
   * Predicted winner identifier captured when the bet was placed. -- GETTER -- Returns the
   * predicted winner identifier.
   */
  @Column(name = "event_winner_id", nullable = false, length = 64)
  private String eventWinnerId;

  /** Monetary stake amount for the persisted bet. -- GETTER -- Returns the stake amount. */
  @Column(name = "bet_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal betAmount;

  /**
   * Timestamp recording when the bet row was created. -- GETTER -- Returns the creation timestamp.
   */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Creates an empty entity instance for JPA. */
  public BetEntity() {}

  /**
   * Creates a fully populated bet entity.
   *
   * @param betId unique bet identifier
   * @param userId unique user identifier
   * @param eventId related event identifier
   * @param eventMarketId related event market identifier
   * @param eventWinnerId predicted winner identifier for the bet
   * @param betAmount stake amount
   * @param createdAt creation timestamp
   */
  public BetEntity(
      String betId,
      String userId,
      String eventId,
      String eventMarketId,
      String eventWinnerId,
      BigDecimal betAmount,
      Instant createdAt) {
    this.betId = betId;
    this.userId = userId;
    this.eventId = eventId;
    this.eventMarketId = eventMarketId;
    this.eventWinnerId = eventWinnerId;
    this.betAmount = betAmount;
    this.createdAt = createdAt;
  }
}
