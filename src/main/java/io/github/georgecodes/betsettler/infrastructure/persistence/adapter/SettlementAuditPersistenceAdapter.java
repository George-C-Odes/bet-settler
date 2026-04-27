package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import io.github.georgecodes.betsettler.infrastructure.persistence.mapper.SettlementAuditEntityMapper;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Spring Data JPA implementation of the settlement audit port. */
@Component
@Transactional
public class SettlementAuditPersistenceAdapter implements SettlementAuditPort {

  /** Publish states that should be replayed by the manual settlement retry flow. */
  private static final List<SettlementPublishStatus> RETRYABLE_PUBLISH_STATUSES =
      List.of(SettlementPublishStatus.PENDING, SettlementPublishStatus.FAILED);

  /** Spring Data repository used to store settlement audit rows. */
  private final SpringDataSettlementAuditRepository settlementAuditRepository;

  /** Shared UTC clock used to timestamp settlement audit writes and updates. */
  private final Clock persistenceClock;

  /**
   * Creates a new settlement audit persistence adapter.
   *
   * @param settlementAuditRepository Spring Data repository used to store settlement audits
   * @param persistenceClock shared clock used to timestamp writes and updates
   */
  public SettlementAuditPersistenceAdapter(
      SpringDataSettlementAuditRepository settlementAuditRepository, Clock persistenceClock) {
    this.settlementAuditRepository =
        Objects.requireNonNull(
            settlementAuditRepository, "settlementAuditRepository must not be null");
    this.persistenceClock =
        Objects.requireNonNull(persistenceClock, "persistenceClock must not be null");
  }

  @Override
  public void savePendingSettlements(List<PendingSettlementAudit> pendingSettlements) {
    Objects.requireNonNull(pendingSettlements, "pendingSettlements must not be null");
    if (pendingSettlements.isEmpty()) {
      return;
    }

    Instant createdAt = Instant.now(persistenceClock);
    settlementAuditRepository.saveAll(
        pendingSettlements.stream()
            .map(
                pendingSettlement ->
                    SettlementAuditEntityMapper.toPendingEntity(
                        pendingSettlement.settlement(),
                        pendingSettlement.destinationTopic(),
                        pendingSettlement.payloadSnapshot(),
                        createdAt))
            .toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<RetryableSettlementAudit> findRetryableSettlements() {
    return settlementAuditRepository
        .findAllByPublishStatusInOrderByCreatedAtAscBetIdAsc(RETRYABLE_PUBLISH_STATUSES)
        .stream()
        .map(this::toRetryableSettlementAudit)
        .toList();
  }

  @Override
  public void markSent(String eventId, String betId) {
    String normalizedEventId = normalizeRequiredValue(eventId, "eventId");
    String normalizedBetId = normalizeRequiredValue(betId, "betId");
    ensureAuditExists(
        normalizedEventId,
        normalizedBetId,
        settlementAuditRepository.updatePublishState(
            normalizedEventId,
            normalizedBetId,
            SettlementPublishStatus.SENT,
            Instant.now(persistenceClock),
            null));
  }

  @Override
  public void markFailed(String eventId, String betId, String failureReason) {
    String normalizedEventId = normalizeRequiredValue(eventId, "eventId");
    String normalizedBetId = normalizeRequiredValue(betId, "betId");
    ensureAuditExists(
        normalizedEventId,
        normalizedBetId,
        settlementAuditRepository.updatePublishState(
            normalizedEventId,
            normalizedBetId,
            SettlementPublishStatus.FAILED,
            Instant.now(persistenceClock),
            normalizeRequiredValue(failureReason, "failureReason")));
  }

  private static void ensureAuditExists(String eventId, String betId, int updatedRowCount) {
    if (updatedRowCount == 1) {
      return;
    }
    throw new IllegalStateException(
        "No settlement audit exists for eventId=" + eventId + " and betId=" + betId);
  }

  private static String normalizeRequiredValue(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }

  private RetryableSettlementAudit toRetryableSettlementAudit(SettlementAuditEntity auditEntity) {
    return new RetryableSettlementAudit(
        auditEntity.getEventId(),
        auditEntity.getBetId(),
        auditEntity.getUserId(),
        auditEntity.getPublishStatus().name(),
        auditEntity.getDestinationTopic(),
        auditEntity.getPayloadSnapshot());
  }
}
