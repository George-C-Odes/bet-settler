package io.github.georgecodes.betsettler.application.port.in;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;

/** Resets the local or test demo state. */
public interface ResetDemoStateUseCase {

  /**
   * Resets the demo state used for local and test execution.
   *
   * @return summary of the transient state that was cleared and the seeded data that was preserved
   */
  DemoResetSummary resetDemoState();
}
