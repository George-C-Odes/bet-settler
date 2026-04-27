package io.github.georgecodes.betsettler.domain.model;

/** Supported settlement outcomes for a bet. */
public enum SettlementResult {

  /** Indicates that the bet winner matches the event winner. */
  WIN,

  /** Indicates that the bet winner does not match the event winner. */
  LOSE
}
