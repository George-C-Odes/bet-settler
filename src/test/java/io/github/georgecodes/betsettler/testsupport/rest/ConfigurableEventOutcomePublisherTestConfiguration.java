package io.github.georgecodes.betsettler.testsupport.rest;

import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.application.port.out.EventOutcomePublisherPort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class ConfigurableEventOutcomePublisherTestConfiguration {

  @Bean
  @Primary
  ConfigurableEventOutcomePublisher configurableEventOutcomePublisher() {
    return new ConfigurableEventOutcomePublisher();
  }

  @Bean
  @Primary
  EventOutcomePublisherPort eventOutcomePublisherPort(
      ConfigurableEventOutcomePublisher configurableEventOutcomePublisher) {
    return configurableEventOutcomePublisher::publish;
  }

  public static final class ConfigurableEventOutcomePublisher {

    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

    public void publish(EventOutcome ignoredEventOutcome) {
      RuntimeException currentFailure = failure.get();
      if (currentFailure != null) {
        throw currentFailure;
      }
    }

    //noinspection unused
    public void failWith(EventOutcomePublishFailedException exception) {
      failure.set(exception);
    }

    //noinspection unused
    public void succeed() {
      failure.set(null);
    }
  }
}
