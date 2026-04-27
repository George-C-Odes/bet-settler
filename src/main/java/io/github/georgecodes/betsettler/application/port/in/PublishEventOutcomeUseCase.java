package io.github.georgecodes.betsettler.application.port.in;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;

/** Publishes an event outcome into the settlement workflow. */
public interface PublishEventOutcomeUseCase {

  /**
   * Publishes a validated event outcome command.
   *
   * @param command event outcome data to publish
   */
  void publish(PublishEventOutcomeCommand command);
}
