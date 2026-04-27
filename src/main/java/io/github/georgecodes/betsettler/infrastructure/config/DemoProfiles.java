package io.github.georgecodes.betsettler.infrastructure.config;

import java.util.List;

/**
 * Centralizes the supported local and test-oriented Spring profiles that expose demo-only
 * capabilities.
 */
public final class DemoProfiles {

  /** Docker-backed local profile. */
  public static final String LOCAL_DOCKER = "local-docker";

  /** Host-run local profile with logging-based settlement publishing. */
  public static final String LOCAL_FALLBACK = "local-fallback";

  /** Automated-test profile. */
  public static final String TEST = "test";

  /** Immutable list of profiles that expose the demo reset endpoint. */
  public static final List<String> DEMO_RESET_ENDPOINT_PROFILES =
      List.of(LOCAL_DOCKER, LOCAL_FALLBACK, TEST);

  /** Prevents instantiation of the profile constants utility type. */
  private DemoProfiles() {}
}
