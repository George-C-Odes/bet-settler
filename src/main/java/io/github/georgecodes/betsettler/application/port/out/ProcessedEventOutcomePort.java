package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;

/** Tracks event outcomes that have already been processed. */
public interface ProcessedEventOutcomePort {

  /**
   * Records that the supplied event outcome has been processed when it is not already present.
   *
   * @param eventOutcome processed event outcome
   * @return {@code true} when the event outcome was newly recorded, otherwise {@code false}
   */
  boolean recordProcessedIfAbsent(EventOutcome eventOutcome);
}
