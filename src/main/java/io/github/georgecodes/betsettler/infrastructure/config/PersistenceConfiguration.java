package io.github.georgecodes.betsettler.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes shared persistence-facing infrastructure helpers. */
@Configuration(proxyBeanMethods = false)
public class PersistenceConfiguration {

  /** Creates a new persistence configuration. */
  public PersistenceConfiguration() {}

  /**
   * Creates the shared UTC clock used by persistence adapters when writing timestamps.
   *
   * @return shared UTC clock
   */
  @Bean
  public Clock persistenceClock() {
    return Clock.systemUTC();
  }
}
