package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;

/** Publishes freshly prepared settlement decisions to the configured outbound destination. */
public interface BetSettlementDispatchPort {

  /**
   * Publishes the supplied settlement decision.
   *
   * @param settlement settlement decision to publish
   */
  void publish(BetSettlement settlement);
}
