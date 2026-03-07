package io.github.opendonationassistant.donationgoal.listeners.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class HistoryItemEventHandlerTest {

  private final ObjectMapper mapper = ObjectMapper.getDefault();
  private final GoalRepository repository = mock(GoalRepository.class);
  private final HistoryFacade facade = mock(HistoryFacade.class);
  private final HistoryItemEventHandler handler = new HistoryItemEventHandler(
    mapper,
    repository,
    facade
  );

  @Test
  @DisplayName(
    "Should send GoalHistoryEvent when valid message with existing goal is handled"
  )
  void handle_validMessage_sendsGoalHistoryEvent() throws IOException {
    // TODO use @Given args in method parameters instead
    final var originId = "test-origin-id";
    final var widgetId = "test-widget-id";
    final var goalId = "test-goal-id";
    final var briefDescription = "Test Goal";

    final var goalData = new GoalData(
      goalId,
      "recipient-id",
      widgetId,
      briefDescription,
      "Full description",
      new Amount(100, 0, "RUB"),
      new Amount(1000, 0, "RUB"),
      true,
      false
    );

    // TODO replace Goal mock with real object and mocked deps
    final var goal = mock(Goal.class);
    when(goal.data()).thenReturn(goalData);
    when(repository.getByOriginId(originId)).thenReturn(Optional.of(goal));

    // TODO inline lines 58-75
    final var event = new HistoryItemEvent(
      "event-id",
      "donation",
      "recipient-id",
      "system",
      originId,
      Instant.now(),
      "nickname",
      new Amount(100, 0, "RUB"),
      "message",
      java.util.List.of(),
      java.util.List.of(),
      null
    );

    final var message = mapper.writeValueAsBytes(event);

    handler.handle(message);

    // TODO replace with argThat
    final var captor = ArgumentCaptor.forClass(GoalHistoryEvent.class);
    verify(facade).sendEvent(captor.capture());

    final var sentEvent = captor.getValue();
    assertEquals("payment", sentEvent.source());
    assertEquals(originId, sentEvent.originId());
    assertEquals(widgetId, sentEvent.widgetId());
    assertEquals(goalId, sentEvent.goalId());
    assertEquals(briefDescription, sentEvent.title());

    verify(repository).getByOriginId(originId);
  }

  @Test
  @DisplayName("Should return early when originId is null")
  void handle_nullOriginId_returnsEarly() throws IOException {
    // TODO inline lines 94-110
    final var event = new HistoryItemEvent(
      "event-id",
      "donation",
      "recipient-id",
      "system",
      null,
      Instant.now(),
      "nickname",
      new Amount(100, 0, "RUB"),
      "message",
      java.util.List.of(),
      java.util.List.of(),
      null
    );

    final var message = mapper.writeValueAsBytes(event);

    handler.handle(message);

    verify(repository, never()).getByOriginId(any());
    verify(facade, never()).sendEvent(any());
  }

  @Test
  @DisplayName("Should not send event when goal is not found")
  void handle_goalNotFound_noEventSent() throws IOException {
    final var originId = "non-existent-origin-id";
    when(repository.getByOriginId(any())).thenReturn(Optional.empty());

    // TODO inline lines 124-140
    final var event = new HistoryItemEvent(
      "event-id",
      "donation",
      "recipient-id",
      "system",
      originId,
      Instant.now(),
      "nickname",
      new Amount(100, 0, "RUB"),
      "message",
      java.util.List.of(),
      java.util.List.of(),
      null
    );

    final var message = mapper.writeValueAsBytes(event);

    handler.handle(message);

    verify(repository).getByOriginId(originId);
    verify(facade, never()).sendEvent(any());
  }

  @Test
  @DisplayName(
    "Should use empty string for title when goal has null briefDescription"
  )
  void handle_goalWithNullDescription_usesEmptyString() throws IOException {
    final var originId = "test-origin-id";
    final var widgetId = "test-widget-id";
    final var goalId = "test-goal-id";

    final var goalData = new GoalData(
      goalId,
      "recipient-id",
      widgetId,
      null,
      "Full description",
      new Amount(100, 0, "RUB"),
      new Amount(1000, 0, "RUB"),
      true,
      false
    );

    // TODO replace Goal mock with real object and mocked deps
    final var goal = mock(Goal.class);
    when(goal.data()).thenReturn(goalData);
    when(repository.getByOriginId(originId)).thenReturn(Optional.of(goal));

    final var event = new HistoryItemEvent(
      "event-id",
      "donation",
      "recipient-id",
      "system",
      originId,
      Instant.now(),
      "nickname",
      new Amount(100, 0, "RUB"),
      "message",
      java.util.List.of(),
      java.util.List.of(),
      null
    );

    // TODO inline message
    final var message = mapper.writeValueAsBytes(event);

    handler.handle(message);

    // TODO replace ArgumentCaptor with argThat
    final var captor = ArgumentCaptor.forClass(GoalHistoryEvent.class);
    verify(facade).sendEvent(captor.capture());

    final var sentEvent = captor.getValue();
    assertEquals("", sentEvent.title());
  }
}
