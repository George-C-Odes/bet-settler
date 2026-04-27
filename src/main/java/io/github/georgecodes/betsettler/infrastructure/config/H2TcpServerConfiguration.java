package io.github.georgecodes.betsettler.infrastructure.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Exposes the in-memory H2 database over TCP for local development profiles. */
@Configuration(proxyBeanMethods = false)
@Profile({DemoProfiles.LOCAL_DOCKER, DemoProfiles.LOCAL_FALLBACK})
@EnableConfigurationProperties(H2TcpServerProperties.class)
public class H2TcpServerConfiguration {

  /** Creates a new H2 TCP server configuration. */
  public H2TcpServerConfiguration() {}

  /**
   * Starts an H2 TCP server so host-side database clients can connect to the running app.
   *
   * @param properties externalized H2 TCP server settings
   * @return started H2 TCP server instance
   * @throws SQLException when the TCP server cannot be created
   */
  @Bean(initMethod = "start", destroyMethod = "stop")
  @ConditionalOnProperty(prefix = "betsettler.h2.tcp", name = "enabled", havingValue = "true")
  public Server h2TcpServer(H2TcpServerProperties properties) throws SQLException {
    return Server.createTcpServer(
        "-tcp", "-tcpAllowOthers", "-tcpPort", Integer.toString(properties.port()));
  }
}
