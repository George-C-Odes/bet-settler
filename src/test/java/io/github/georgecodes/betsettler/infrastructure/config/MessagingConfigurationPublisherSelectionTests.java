package io.github.georgecodes.betsettler.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher.RocketMqBetSettlementPublisher;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher.LoggingBetSettlementPublisher;
import java.util.Map;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class MessagingConfigurationPublisherSelectionTests {

  private final ApplicationContextRunner contextRunnerWithRocketMqTemplate =
      new ApplicationContextRunner()
          .withUserConfiguration(PublisherSelectionWithRocketMqTemplateConfiguration.class);

  private final ApplicationContextRunner contextRunnerWithoutRocketMqTemplate =
      new ApplicationContextRunner()
          .withUserConfiguration(PublisherSelectionWithoutRocketMqTemplateConfiguration.class);

  @Test
  void defaultsToLoggingPublisherWhenPublisherModeIsMissing() {
    contextRunnerWithRocketMqTemplate.run(this::assertSingleLoggingPublisher);
  }

  @Test
  void selectsLoggingPublisherWhenLoggingModeIsConfigured() {
    contextRunnerWithRocketMqTemplate
        .withPropertyValues("betsettler.messaging.rocketmq.publisher-mode=logging")
        .run(this::assertSingleLoggingPublisher);
  }

  @Test
  void selectsRocketMqPublisherWhenRocketMqModeIsConfigured() {
    contextRunnerWithRocketMqTemplate
        .withPropertyValues("betsettler.messaging.rocketmq.publisher-mode=rocketmq")
        .run(
            context -> {
              Map<String, BetSettlementDispatchPort> dispatchPorts =
                  context.getBeansOfType(BetSettlementDispatchPort.class);
              Map<String, BetSettlementReplayPort> replayPorts =
                  context.getBeansOfType(BetSettlementReplayPort.class);
              assertThat(dispatchPorts).hasSize(1);
              assertThat(replayPorts).hasSize(1);
              assertThat(dispatchPorts.values())
                  .singleElement()
                  .isInstanceOf(RocketMqBetSettlementPublisher.class);
              assertThat(replayPorts.values())
                  .singleElement()
                  .isInstanceOf(RocketMqBetSettlementPublisher.class);
            });
  }

  @Test
  void failsStartupWhenRocketMqModeIsConfiguredWithoutRocketMqTemplate() {
    contextRunnerWithoutRocketMqTemplate
        .withPropertyValues("betsettler.messaging.rocketmq.publisher-mode=rocketmq")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                  .hasMessageContaining(RocketMQTemplate.class.getName());
            });
  }

  private void assertSingleLoggingPublisher(AssertableApplicationContext context) {
    Map<String, BetSettlementDispatchPort> dispatchPorts =
        context.getBeansOfType(BetSettlementDispatchPort.class);
    Map<String, BetSettlementReplayPort> replayPorts =
        context.getBeansOfType(BetSettlementReplayPort.class);
    assertThat(dispatchPorts).hasSize(1);
    assertThat(replayPorts).hasSize(1);
    assertThat(dispatchPorts.values())
        .singleElement()
        .isInstanceOf(LoggingBetSettlementPublisher.class);
    assertThat(replayPorts.values())
        .singleElement()
        .isInstanceOf(LoggingBetSettlementPublisher.class);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(BetSettlerMessagingProperties.class)
  @Import(MessagingConfiguration.class)
  static class PublisherSelectionWithoutRocketMqTemplateConfiguration {

    @Bean
    BetSettlementMessageMapper betSettlementMessageMapper() {
      return new BetSettlementMessageMapper();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(BetSettlerMessagingProperties.class)
  @Import(MessagingConfiguration.class)
  static class PublisherSelectionWithRocketMqTemplateConfiguration {

    @Bean
    BetSettlementMessageMapper betSettlementMessageMapper() {
      return new BetSettlementMessageMapper();
    }

    @Bean
    RocketMQTemplate rocketMqTemplate() {
      return mock(RocketMQTemplate.class);
    }
  }
}
