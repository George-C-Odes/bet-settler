package io.github.georgecodes.betsettler.application.model.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreparedDispatchPlanTests {

  private static final Instant CREATED_AT = Instant.parse("2026-04-23T10:15:30Z");

  @Test
  void duplicatePlanExposesDuplicateStateWithoutDestinationTopic() {
    PreparedDispatchPlan preparedDispatchPlan = PreparedDispatchPlan.duplicate("EVT-8001");

    assertThat(preparedDispatchPlan.eventId()).isEqualTo("EVT-8001");
    assertThat(preparedDispatchPlan.status()).isEqualTo(PreparedDispatchPlan.Status.DUPLICATE);
    assertThat(preparedDispatchPlan.isDuplicate()).isTrue();
    assertThat(preparedDispatchPlan.requiresDispatch()).isFalse();
    assertThat(preparedDispatchPlan.hasDestinationTopic()).isFalse();
    assertThat(preparedDispatchPlan.settlements()).isEmpty();
  }

  @Test
  void duplicatePlanRejectsRequiredDestinationTopicAccess() {
    PreparedDispatchPlan preparedDispatchPlan = PreparedDispatchPlan.duplicate("EVT-8002");

    assertThatThrownBy(preparedDispatchPlan::requiredDestinationTopic)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("destinationTopic is unavailable for status=DUPLICATE");
  }

  @Test
  void noMatchPlanExposesDestinationTopicWithoutRequiringDispatch() {
    PreparedDispatchPlan preparedDispatchPlan =
        PreparedDispatchPlan.noMatch("EVT-9001", "bet-settlements");

    assertThat(preparedDispatchPlan.status()).isEqualTo(PreparedDispatchPlan.Status.NO_MATCH);
    assertThat(preparedDispatchPlan.isDuplicate()).isFalse();
    assertThat(preparedDispatchPlan.requiresDispatch()).isFalse();
    assertThat(preparedDispatchPlan.hasDestinationTopic()).isTrue();
    assertThat(preparedDispatchPlan.requiredDestinationTopic()).isEqualTo("bet-settlements");
    assertThat(preparedDispatchPlan.settlements()).isEmpty();
  }

  @Test
  void dispatchPlanCopiesSettlementsAndExposesDispatchState() {
    BetSettlement settlement = createWinningSettlement();
    List<BetSettlement> mutableSettlements = new ArrayList<>(List.of(settlement));

    PreparedDispatchPlan preparedDispatchPlan =
        PreparedDispatchPlan.dispatch("EVT-1001", "bet-settlements", mutableSettlements);
    mutableSettlements.clear();

    assertThat(preparedDispatchPlan.status())
        .isEqualTo(PreparedDispatchPlan.Status.DISPATCH_REQUIRED);
    assertThat(preparedDispatchPlan.isDuplicate()).isFalse();
    assertThat(preparedDispatchPlan.requiresDispatch()).isTrue();
    assertThat(preparedDispatchPlan.hasDestinationTopic()).isTrue();
    assertThat(preparedDispatchPlan.requiredDestinationTopic()).isEqualTo("bet-settlements");
    assertThat(preparedDispatchPlan.settlements()).containsExactly(settlement);
    assertThatThrownBy(() -> preparedDispatchPlan.settlements().add(settlement))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void dispatchPlanRejectsEmptySettlements() {
    assertThatThrownBy(
            () -> PreparedDispatchPlan.dispatch("EVT-1001", "bet-settlements", List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("settlements must not be empty when status=DISPATCH_REQUIRED");
  }

  private BetSettlement createWinningSettlement() {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    Bet bet =
        new Bet(
            "BET-1001",
            "USER-1",
            "EVT-1001",
            "MARKET-1",
            "TEAM-A",
            new BigDecimal("25.00"),
            CREATED_AT);
    return new BetSettlement(bet, eventOutcome, SettlementResult.WIN);
  }
}
