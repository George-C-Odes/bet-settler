package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.domain.model.Bet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BetResponseMapperTests {

  private static final Instant CREATED_AT = Instant.parse("2026-04-27T10:15:30Z");

  private final BetResponseMapper betResponseMapper = new BetResponseMapper();

  @Test
  void toBetResponseMapsTheDomainBet() {
    assertThat(betResponseMapper.toBetResponse(sampleBet()))
        .extracting(
            "betId",
            "userId",
            "eventId",
            "eventMarketId",
            "eventWinnerId",
            "betAmount",
            "createdAt")
        .containsExactly(
            "BET-1001",
            "USER-1",
            "EVT-1001",
            "MKT-1001",
            "TEAM-A",
            new BigDecimal("25.00"),
            CREATED_AT);
  }

  @Test
  void toBetResponsesMapsEveryBetInOrder() {
    assertThat(betResponseMapper.toBetResponses(List.of(sampleBet(), sampleBet("BET-1002"))))
        .extracting("betId")
        .containsExactly("BET-1001", "BET-1002");
  }

  @Test
  void toBetResponseRejectsNullBet() {
    assertThatThrownBy(() -> betResponseMapper.toBetResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("bet must not be null");
  }

  @Test
  void toBetResponsesRejectsNullBetList() {
    assertThatThrownBy(() -> betResponseMapper.toBetResponses(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("bets must not be null");
  }

  private static Bet sampleBet() {
    return sampleBet("BET-1001");
  }

  private static Bet sampleBet(String betId) {
    return new Bet(
        betId, "USER-1", "EVT-1001", "MKT-1001", "TEAM-A", new BigDecimal("25.00"), CREATED_AT);
  }
}
