package io.github.opendonationassistant.donationgoal.listeners.handlers;

import static org.instancio.Select.field;
import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalDataRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalLinkRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(InstancioExtension.class)
public class HistoryItemEventHandlerTest {

  private final ObjectMapper mapper = ObjectMapper.getDefault();
  private final GoalRepository repository = mock(GoalRepository.class);
  private final HistoryFacade facade = mock(HistoryFacade.class);
  private final HistoryItemEventHandler handler = new HistoryItemEventHandler(
    mapper,
    repository,
    facade
  );

  Model<HistoryItemEvent> historyItemEventModel = Instancio.of(
    HistoryItemEvent.class
  )
    .set(field(HistoryItemEvent::actions), List.of())
    .toModel();

  @Test
  @DisplayName(
    "Should send GoalHistoryEvent when valid message with existing goal is handled"
  )
  void handle_validMessage_sendsGoalHistoryEvent() throws IOException {
    // TODO replace with @Given arg in method parameters
    var originId = "test-origin-id";
    var widgetId = "test-widget-id";
    var goalId = "test-goal-id";
    var briefDescription = "Test Goal";
    var recipientId = "test-recipient-id";

    var goalData = new GoalData(
      goalId,
      recipientId,
      widgetId,
      briefDescription,
      "Full description",
      new Amount(100, 0, "RUB"),
      new Amount(1000, 0, "RUB"),
      true,
      false
    );
    var goal = new Goal(
      goalData,
      mock(GoalWidgetCommandSender.class),
      mock(GoalDataRepository.class),
      mock(GoalLinkRepository.class)
    );
    when(repository.getByOriginId(originId)).thenReturn(Optional.of(goal));

    var event = Instancio.of(historyItemEventModel)
      .set(field(HistoryItemEvent::id), "event-id")
      .set(field(HistoryItemEvent::type), "payment")
      .set(field(HistoryItemEvent::originId), originId)
      .set(field(HistoryItemEvent::recipientId), recipientId)
      .set(field(HistoryItemEvent::amount), new Amount(100, 0, "RUB"))
      .create();

    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade).sendEvent(
      argThat(
        (GoalHistoryEvent e) ->
          "payment".equals(e.source()) &&
          originId.equals(e.originId()) &&
          widgetId.equals(e.widgetId()) &&
          goalId.equals(e.goalId()) &&
          briefDescription.equals(e.title())
      )
    );

    verify(repository).getByOriginId(originId);
  }

  @Test
  @DisplayName("Should return early when originId is null")
  void handle_nullOriginId_returnsEarly() throws IOException {
    var event = Instancio.of(historyItemEventModel)
      .set(field(HistoryItemEvent::originId), null)
      .create();
    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade, never()).sendEvent(any());
  }

  @Test
  @DisplayName("Should not send event when goal is not found")
  void handle_goalNotFound_noEventSent() throws IOException {
    when(repository.getByOriginId(any())).thenReturn(Optional.empty());

    var event = Instancio.of(historyItemEventModel).create();

    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade, never()).sendEvent(any());
  }

  @Test
  @DisplayName(
    "Should use empty string for title when goal has null briefDescription"
  )
  void handle_goalWithNullDescription_usesEmptyString() throws IOException {
    var originId = "test-origin-id";
    var widgetId = "test-widget-id";
    var goalId = "test-goal-id";

    var goalData = new GoalData(
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
    var goal = new Goal(
      goalData,
      mock(GoalWidgetCommandSender.class),
      mock(GoalDataRepository.class),
      mock(GoalLinkRepository.class)
    );
    when(repository.getByOriginId(originId)).thenReturn(Optional.of(goal));

    var event = new HistoryItemEvent(
      "event-id",
      "donation",
      "recipient-id",
      "system",
      originId,
      Instant.now(),
      "nickname",
      new Amount(100, 0, "RUB"),
      "message",
      List.of(),
      List.of(),
      null
    );

    handler.handle(mapper.writeValueAsBytes(event));

    verify(facade).sendEvent(
      argThat((GoalHistoryEvent e) -> e.title().equals(""))
    );
  }
}
