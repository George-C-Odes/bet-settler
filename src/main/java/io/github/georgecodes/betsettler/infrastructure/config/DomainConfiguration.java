package io.github.georgecodes.betsettler.infrastructure.config;

import io.github.georgecodes.betsettler.domain.service.BetSettlementDecider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers domain-service beans used by the application layer without introducing Spring into the
 * domain.
 */
@Configuration(proxyBeanMethods = false)
public class DomainConfiguration {

  /** Creates a new domain configuration. */
  public DomainConfiguration() {}

  /**
   * Creates the stateless bet-settlement decider used by settlement-preparation flows.
   *
   * @return stateless bet-settlement decider
   */
  @Bean
  public BetSettlementDecider betSettlementDecider() {
    return new BetSettlementDecider();
  }
}
