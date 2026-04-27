package io.github.georgecodes.betsettler.application.port.in;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;

/** Processes an event outcome received for settlement. */
public interface ProcessEventOutcomeUseCase {

  /**
   * Processes the supplied event outcome.
   *
   * @param eventOutcome event outcome to process
   */
  void process(EventOutcome eventOutcome);
}
