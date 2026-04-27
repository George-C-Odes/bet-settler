package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BetQueryPersistenceAdapterIntegrationTests {

  @Autowired private BetQueryPort betQueryPort;

  @Test
  void getBetsReturnsAllSeededBetsInIdentifierOrder() {
    assertThat(betQueryPort.getAllBets())
        .extracting(Bet::betId)
        .containsExactly("BET-1001", "BET-1002", "BET-2001", "BET-3001");
  }

  @Test
  void getBetsFiltersByEventIdAndReturnsEmptyForUnknownEvent() {
    assertThat(betQueryPort.getBetsByEventId(" EVT-1001 "))
        .extracting(Bet::betId)
        .containsExactly("BET-1001", "BET-1002");

    assertThat(betQueryPort.getBetsByEventId("EVT-9999")).isEmpty();
    assertThatThrownBy(() -> betQueryPort.getBetsByEventId("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("eventId must not be blank");
  }
}
