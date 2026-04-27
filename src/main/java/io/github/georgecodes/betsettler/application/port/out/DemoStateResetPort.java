package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;

/** Resets the local or test demo state maintained by infrastructure adapters. */
public interface DemoStateResetPort {

  /**
   * Resets transient demo state while preserving seeded reference data.
   *
   * @return summary of cleared transient rows and preserved seeded bets
   */
  DemoResetSummary resetDemoState();
}
