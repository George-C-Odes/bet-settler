package io.github.georgecodes.betsettler.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class H2TcpServerPropertiesTests {

  @Test
  void defaultsPortWhenZeroValueIsProvided() {
    H2TcpServerProperties properties = new H2TcpServerProperties(true, 0);

    assertThat(properties.enabled()).isTrue();
    assertThat(properties.port()).isEqualTo(H2TcpServerProperties.DEFAULT_PORT);
  }

  @Test
  void acceptsExplicitTcpPort() {
    H2TcpServerProperties properties = new H2TcpServerProperties(true, 9191);

    assertThat(properties.port()).isEqualTo(9191);
  }

  @Test
  void rejectsPortsOutsideTcpRange() {
    assertThatThrownBy(() -> new H2TcpServerProperties(true, 65_536))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("betsettler.h2.tcp.port must be between 1 and 65535");
  }

  @Test
  void rejectsPortsBelowTcpRange() {
    assertThatThrownBy(() -> new H2TcpServerProperties(true, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("betsettler.h2.tcp.port must be between 1 and 65535");
  }
}
