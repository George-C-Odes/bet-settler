package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ProcessedEventOutcomePersistenceAdapterTests {

  private static final Instant PROCESSED_AT = Instant.parse("2026-04-27T11:45:30Z");

  @Mock private SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  private ProcessedEventOutcomePersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new ProcessedEventOutcomePersistenceAdapter(
            processedEventOutcomeRepository, Clock.fixed(PROCESSED_AT, ZoneOffset.UTC));
  }

  @Test
  void recordProcessedIfAbsentReturnsFalseWhenInsertHitsConstraintRace() {
    EventOutcome eventOutcome = new EventOutcome("EVT-9001", "Team O vs Team P", "TEAM-O");
    when(processedEventOutcomeRepository.insertProcessedEventOutcome(any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("duplicate event outcome"));

    boolean recorded = adapter.recordProcessedIfAbsent(eventOutcome);

    assertThat(recorded).isFalse();
    verify(processedEventOutcomeRepository, never()).existsById(any());
    verify(processedEventOutcomeRepository).insertProcessedEventOutcome(any(), any(), any(), any());
  }

  @Test
  void recordProcessedIfAbsentReturnsFalseWhenInsertReportsExistingEvent() {
    EventOutcome eventOutcome = new EventOutcome("EVT-9002", "Team Q vs Team R", "TEAM-Q");
    when(processedEventOutcomeRepository.insertProcessedEventOutcome(any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("duplicate event outcome"));

    boolean recorded = adapter.recordProcessedIfAbsent(eventOutcome);

    assertThat(recorded).isFalse();
    verify(processedEventOutcomeRepository, never()).existsById(any());
    verify(processedEventOutcomeRepository).insertProcessedEventOutcome(any(), any(), any(), any());
  }

  @Test
  void recordProcessedIfAbsentReturnsFalseWhenInsertAffectsNoRows() {
    EventOutcome eventOutcome = new EventOutcome("EVT-9003", "Team S vs Team T", "TEAM-S");
    when(processedEventOutcomeRepository.insertProcessedEventOutcome(any(), any(), any(), any()))
        .thenReturn(0);

    boolean recorded = adapter.recordProcessedIfAbsent(eventOutcome);

    assertThat(recorded).isFalse();
    verify(processedEventOutcomeRepository, never()).existsById(any());
    verify(processedEventOutcomeRepository).insertProcessedEventOutcome(any(), any(), any(), any());
  }

  @Test
  void recordProcessedIfAbsentReturnsTrueWhenInsertSucceeds() {
    EventOutcome eventOutcome = new EventOutcome("EVT-9004", "Team U vs Team V", "TEAM-U");
    when(processedEventOutcomeRepository.insertProcessedEventOutcome(any(), any(), any(), any()))
        .thenReturn(1);

    boolean recorded = adapter.recordProcessedIfAbsent(eventOutcome);

    assertThat(recorded).isTrue();
    verify(processedEventOutcomeRepository, never()).existsById(any());
    verify(processedEventOutcomeRepository)
        .insertProcessedEventOutcome(
            any(),
            any(),
            any(),
            assertArg(timestamp -> assertThat(timestamp).isEqualTo(PROCESSED_AT)));
  }
}
