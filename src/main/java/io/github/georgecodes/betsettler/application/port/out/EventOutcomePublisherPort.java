package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;

/** Publishes event outcomes to an external transport. */
public interface EventOutcomePublisherPort {

  /**
   * Publishes the supplied event outcome.
   *
   * @param eventOutcome event outcome to publish
   */
  void publish(EventOutcome eventOutcome);
}
