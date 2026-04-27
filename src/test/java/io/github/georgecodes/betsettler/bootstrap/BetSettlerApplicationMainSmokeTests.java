package io.github.georgecodes.betsettler.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.georgecodes.betsettler.BetSettlerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class BetSettlerApplicationMainSmokeTests {

  @Test
  void mainBootstrapsTheApplication() {
    SpringApplication.Running running =
        SpringApplication.from(BetSettlerApplication::main)
            .run(
                "--server.port=0",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=test");
    try (ConfigurableApplicationContext context = running.getApplicationContext()) {
      assertThat(context).isNotNull();
      assertThat(context.isActive()).isTrue();
    }
  }
}
