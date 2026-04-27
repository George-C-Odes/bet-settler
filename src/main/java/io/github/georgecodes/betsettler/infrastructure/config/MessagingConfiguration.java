package io.github.georgecodes.betsettler.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementPayloadPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher.RocketMqBetSettlementPublisher;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher.LoggingBetSettlementPublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/** Registers messaging-related infrastructure configuration. */
@Configuration(proxyBeanMethods = false)
@EnableKafka
@EnableConfigurationProperties(BetSettlerMessagingProperties.class)
public class MessagingConfiguration {

  /** Creates a new messaging configuration. */
  public MessagingConfiguration() {}

  /**
   * Creates the shared JSON mapper used by settlement payload snapshot and replay components.
   *
   * @return shared application JSON mapper
   */
  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }

  /**
   * Creates the shared settlement payload codec used by outbound publishers and snapshot creation.
   *
   * @param betSettlementMessageMapper mapper used to create the shared settlement message contract
   * @param objectMapper shared application JSON mapper
   * @return shared settlement payload codec
   */
  @Bean
  BetSettlementPayloadCodec betSettlementPayloadCodec(
      BetSettlementMessageMapper betSettlementMessageMapper, ObjectMapper objectMapper) {
    return new BetSettlementPayloadCodec(betSettlementMessageMapper, objectMapper);
  }

  /**
   * Creates the snapshot-focused settlement payload factory used by transactional preparation.
   *
   * @param messagingProperties externalized messaging properties
   * @param betSettlementPayloadCodec shared settlement payload codec
   * @return shared settlement payload snapshot factory
   */
  @Bean
  BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory(
      BetSettlerMessagingProperties messagingProperties,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    return new BetSettlementPayloadSnapshotFactory(messagingProperties, betSettlementPayloadCodec);
  }

  /**
   * Exposes the narrower settlement payload abstraction used by transactional preparation.
   *
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @return shared settlement payload abstraction
   */
  @Bean
  BetSettlementPayloadPort betSettlementPayloadPort(
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory) {
    return betSettlementPayloadSnapshotFactory;
  }

  /**
   * Creates the settlement publisher used when RocketMQ mode is enabled.
   *
   * @param rocketMqTemplate RocketMQ transport template required for explicit RocketMQ mode
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @param betSettlementPayloadCodec shared settlement payload codec
   * @return RocketMQ-backed settlement publisher
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "betsettler.messaging.rocketmq",
      name = "publisher-mode",
      havingValue = "rocketmq")
  RocketMqBetSettlementPublisher rocketMqBetSettlementPublisher(
      RocketMQTemplate rocketMqTemplate,
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    return new RocketMqBetSettlementPublisher(
        rocketMqTemplate, betSettlementPayloadSnapshotFactory, betSettlementPayloadCodec);
  }

  /**
   * Creates the logging settlement publisher whenever no other settlement publisher has been
   * configured.
   *
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @param betSettlementPayloadCodec shared settlement payload codec
   * @return logging-based settlement publisher
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "betsettler.messaging.rocketmq",
      name = "publisher-mode",
      havingValue = "logging",
      matchIfMissing = true)
  @ConditionalOnMissingBean({BetSettlementDispatchPort.class, BetSettlementReplayPort.class})
  LoggingBetSettlementPublisher loggingBetSettlementPublisher(
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    return new LoggingBetSettlementPublisher(
        betSettlementPayloadSnapshotFactory, betSettlementPayloadCodec);
  }
}
