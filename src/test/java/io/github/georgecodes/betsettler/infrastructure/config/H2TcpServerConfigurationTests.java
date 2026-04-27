package io.github.georgecodes.betsettler.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import org.h2.tools.Server;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class H2TcpServerConfigurationTests {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(H2TcpServerConfiguration.class);

  @Test
  void createsTcpServerWhenLocalDockerProfileAndTcpSupportAreEnabled() throws IOException {
    int port = findAvailablePort();

    contextRunner
        .withPropertyValues(
            "spring.profiles.active=local-docker",
            "betsettler.h2.tcp.enabled=true",
            "betsettler.h2.tcp.port=" + port)
        .run(
            context -> {
              assertThat(context).hasSingleBean(Server.class);
              Server server = context.getBean(Server.class);
              assertThat(server.getPort()).isEqualTo(port);
              assertThat(server.isRunning(false)).isTrue();
            });
  }

  @Test
  void createsTcpServerWhenLocalFallbackProfileAndTcpSupportAreEnabled() throws IOException {
    int port = findAvailablePort();

    contextRunner
        .withPropertyValues(
            "spring.profiles.active=local-fallback",
            "betsettler.h2.tcp.enabled=true",
            "betsettler.h2.tcp.port=" + port)
        .run(
            context -> {
              assertThat(context).hasSingleBean(Server.class);
              Server server = context.getBean(Server.class);
              assertThat(server.getPort()).isEqualTo(port);
              assertThat(server.isRunning(false)).isTrue();
            });
  }

  @Test
  void doesNotCreateTcpServerWhenTcpSupportIsDisabled() {
    contextRunner
        .withPropertyValues("spring.profiles.active=local-docker")
        .run(context -> assertThat(context).doesNotHaveBean(Server.class));
  }

  private static int findAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
