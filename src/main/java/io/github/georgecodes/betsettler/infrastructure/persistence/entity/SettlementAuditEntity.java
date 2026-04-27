package io.github.georgecodes.betsettler.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/** JPA entity that records settlement publication audit information. */
@Getter
@SuppressWarnings({"unused"})
@Entity
@Table(name = "settlement_audit")
public class SettlementAuditEntity {

  /**
   * Generated surrogate key for the settlement audit row. -- GETTER -- Returns the generated audit
   * identifier.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_id", nullable = false, updatable = false)
  private Long auditId;

  /**
   * Event identifier associated with the settlement attempt. -- GETTER -- Returns the related event
   * identifier.
   */
  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  /**
   * Bet identifier associated with the settlement attempt. -- GETTER -- Returns the related bet
   * identifier.
   */
  @Column(name = "bet_id", nullable = false, length = 64)
  private String betId;

  /**
   * User identifier associated with the settlement attempt. -- GETTER -- Returns the related user
   * identifier.
   */
  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  /**
   * Settlement result snapshot stored for audit purposes. -- GETTER -- Returns the settlement
   * result snapshot.
   */
  @Column(name = "settlement_result", nullable = false, length = 16)
  private String settlementResult;

  /**
   * Destination topic or channel name used for publication. -- GETTER -- Returns the destination
   * topic or channel name.
   */
  @Column(name = "destination_topic", nullable = false, length = 128)
  private String destinationTopic;

  /**
   * Serialized payload snapshot stored with the audit record. -- GETTER -- Returns the serialized
   * payload snapshot.
   */
  @Column(name = "payload_snapshot", nullable = false, length = 4096)
  private String payloadSnapshot;

  /**
   * Publication status persisted for the settlement audit row. -- GETTER -- Returns the publish
   * status.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "publish_status", nullable = false, length = 16)
  private SettlementPublishStatus publishStatus;

  /**
   * Timestamp of the send attempt when one has occurred. -- GETTER -- Returns the publication
   * timestamp.
   */
  @Column(name = "published_at")
  private Instant publishedAt;

  /**
   * Failure reason captured for an unsuccessful send attempt. -- GETTER -- Returns the failure
   * reason.
   */
  @Column(name = "failure_reason", length = 512)
  private String failureReason;

  /**
   * Timestamp recording when the audit row was first created. -- GETTER -- Returns the creation
   * timestamp.
   */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Creates an empty entity instance for JPA. */
  public SettlementAuditEntity() {}

  /**
   * Creates a fully populated pending settlement audit entity.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   * @param userId related user identifier
   * @param settlementResult resulting settlement status
   * @param destinationTopic destination topic or channel name
   * @param payloadSnapshot serialized payload snapshot
   * @param publishStatus current publish status
   * @param createdAt creation timestamp
   */
  public SettlementAuditEntity(
      String eventId,
      String betId,
      String userId,
      String settlementResult,
      String destinationTopic,
      String payloadSnapshot,
      SettlementPublishStatus publishStatus,
      Instant createdAt) {
    this.eventId = eventId;
    this.betId = betId;
    this.userId = userId;
    this.settlementResult = settlementResult;
    this.destinationTopic = destinationTopic;
    this.payloadSnapshot = payloadSnapshot;
    this.publishStatus = publishStatus;
    this.createdAt = createdAt;
  }

  /**
   * Marks the audit entry as sent.
   *
   * @param publishedAt publication timestamp
   */
  public void markSent(Instant publishedAt) {
    this.publishStatus = SettlementPublishStatus.SENT;
    this.publishedAt = publishedAt;
    this.failureReason = null;
  }

  /**
   * Marks the audit entry as failed.
   *
   * @param publishedAt publication timestamp for the failed attempt
   * @param failureReason reason for the failed publication
   */
  public void markFailed(Instant publishedAt, String failureReason) {
    this.publishStatus = SettlementPublishStatus.FAILED;
    this.publishedAt = publishedAt;
    this.failureReason = failureReason;
  }
}
