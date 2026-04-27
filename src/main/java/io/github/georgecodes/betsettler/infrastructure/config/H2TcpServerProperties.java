package io.github.georgecodes.betsettler.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized H2 TCP server settings for local development access.
 *
 * @param enabled whether the H2 TCP server should be started
 * @param port port exposed by the H2 TCP server
 */
@ConfigurationProperties(prefix = "betsettler.h2.tcp")
public record H2TcpServerProperties(boolean enabled, int port) {

  /** Default TCP port exposed for external H2 clients. */
  public static final int DEFAULT_PORT = 9093;

  /**
   * Creates H2 TCP server properties with sensible defaults for local development access.
   *
   * @param enabled whether the H2 TCP server should be started
   * @param port port exposed by the H2 TCP server
   */
  public H2TcpServerProperties {
    port = normalisePort(port);
  }

  private static int normalisePort(int port) {
    if (port == 0) {
      return DEFAULT_PORT;
    }
    if (port < 1 || port > 65_535) {
      throw new IllegalArgumentException("betsettler.h2.tcp.port must be between 1 and 65535");
    }
    return port;
  }
}
