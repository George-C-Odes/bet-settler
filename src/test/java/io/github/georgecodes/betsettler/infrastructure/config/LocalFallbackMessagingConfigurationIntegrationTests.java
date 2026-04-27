package io.github.georgecodes.betsettler.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher.LoggingBetSettlementPublisher;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("local-fallback")
class LocalFallbackMessagingConfigurationIntegrationTests {

  private static final int H2_TCP_PORT = findAvailablePort();

  @Autowired private BetSettlementDispatchPort betSettlementDispatchPort;

  @Autowired private BetSettlementReplayPort betSettlementReplayPort;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("betsettler.h2.tcp.port", () -> H2_TCP_PORT);
  }

  @Test
  void localFallbackProfileSelectsLoggingPublisherForDispatchAndReplay() {
    assertThat(betSettlementDispatchPort).isInstanceOf(LoggingBetSettlementPublisher.class);
    assertThat(betSettlementReplayPort).isInstanceOf(LoggingBetSettlementPublisher.class);
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to allocate an available H2 TCP port for tests", exception);
    }
  }
}
