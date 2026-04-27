package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.georgecodes.betsettler.application.port.out.ProcessedEventOutcomePort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.ProcessedEventOutcomeEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcessedEventOutcomePersistenceAdapterIntegrationTests {

  @Autowired private ProcessedEventOutcomePort processedEventOutcomePort;

  @Autowired private SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  @Test
  void recordProcessedIfAbsentPersistsOutcome() {
    EventOutcome eventOutcome = new EventOutcome(" EVT-4001 ", " Team E vs Team F ", " TEAM-E ");

    assertThat(processedEventOutcomeRepository.findById("EVT-4001")).isEmpty();

    assertThat(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).isTrue();

    assertThat(processedEventOutcomeRepository.findById("EVT-4001"))
        .get()
        .extracting(
            ProcessedEventOutcomeEntity::getEventName,
            ProcessedEventOutcomeEntity::getEventWinnerId,
            ProcessedEventOutcomeEntity::getProcessedAt)
        .containsExactly(
            "Team E vs Team F",
            "TEAM-E",
            processedEventOutcomeRepository.findById("EVT-4001").orElseThrow().getProcessedAt());
  }

  @Test
  void recordProcessedIfAbsentReturnsFalseForDuplicateEventId() {
    EventOutcome eventOutcome = new EventOutcome("EVT-4002", "Team G vs Team H", "TEAM-G");

    assertThat(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).isTrue();
    assertThat(processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)).isFalse();
    assertThat(processedEventOutcomeRepository.count()).isEqualTo(1L);
  }
}
