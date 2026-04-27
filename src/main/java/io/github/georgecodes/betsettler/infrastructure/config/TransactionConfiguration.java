package io.github.georgecodes.betsettler.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** Exposes transaction infrastructure needed by application services. */
@Configuration(proxyBeanMethods = false)
public class TransactionConfiguration {

  /** Creates a new transaction configuration. */
  public TransactionConfiguration() {}

  /**
   * Creates the transaction operations abstraction used by orchestrating application services.
   *
   * @param transactionManager Spring transaction manager backing the application database
   * @return transaction operations abstraction backed by a transaction template
   */
  @Bean
  public TransactionOperations transactionOperations(
      PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
