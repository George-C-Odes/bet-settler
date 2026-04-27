package io.github.georgecodes.betsettler.infrastructure.persistence.entity;

/** Enumerates the persistence-level states of a settlement publication audit entry. */
public enum SettlementPublishStatus {
  /** The audit row has been created but publication has not yet been confirmed. */
  PENDING,

  /** The settlement message has been published successfully. */
  SENT,

  /** The settlement publication attempt failed. */
  FAILED
}
