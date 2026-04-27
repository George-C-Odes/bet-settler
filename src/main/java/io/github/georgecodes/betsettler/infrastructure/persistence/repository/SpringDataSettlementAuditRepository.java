package io.github.georgecodes.betsettler.infrastructure.persistence.repository;

import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for settlement publication audit records. */
@SuppressWarnings("unused")
public interface SpringDataSettlementAuditRepository
    extends JpaRepository<SettlementAuditEntity, Long> {

  /**
   * Returns the audit entry for a specific event and bet pair.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   * @return the matching audit entry when one exists
   */
  Optional<SettlementAuditEntity> findByEventIdAndBetId(String eventId, String betId);

  /**
   * Updates the publish state for a specific event and bet pair.
   *
   * @param eventId related event identifier
   * @param betId related bet identifier
   * @param publishStatus new publish status
   * @param publishedAt timestamp of the publication attempt
   * @param failureReason failure reason, or {@code null} for successful publication
   * @return number of rows updated
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update SettlementAuditEntity settlementAudit
         set settlementAudit.publishStatus = :publishStatus,
             settlementAudit.publishedAt = :publishedAt,
             settlementAudit.failureReason = :failureReason
       where settlementAudit.eventId = :eventId
         and settlementAudit.betId = :betId
      """)
  int updatePublishState(
      @Param("eventId") String eventId,
      @Param("betId") String betId,
      @Param("publishStatus") SettlementPublishStatus publishStatus,
      @Param("publishedAt") Instant publishedAt,
      @Param("failureReason") String failureReason);

  /**
   * Returns all audit entries for an event ordered by bet identifier.
   *
   * @param eventId related event identifier
   * @return matching audit entries ordered by bet identifier
   */
  List<SettlementAuditEntity> findAllByEventIdOrderByBetIdAsc(String eventId);

  /**
   * Returns settlement audit rows that are still retryable ordered by creation time and bet id.
   *
   * @param publishStatuses statuses that should be treated as retryable
   * @return retryable settlement audit rows ordered for replay
   */
  List<SettlementAuditEntity> findAllByPublishStatusInOrderByCreatedAtAscBetIdAsc(
      List<SettlementPublishStatus> publishStatuses);
}
