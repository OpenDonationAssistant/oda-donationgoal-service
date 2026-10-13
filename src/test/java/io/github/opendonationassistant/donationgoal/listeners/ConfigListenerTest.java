package io.github.opendonationassistant.donationgoal.listeners;

import static org.mockito.Mockito.*;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalDataRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalLinkRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalMode;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConfigListenerTest {

  private static final String WIDGET_ID = "test-widget-id";
  private static final String RECIPIENT_ID = "test-recipient-id";
  private static final String GOAL_ID = "test-goal-id";
  private static final Amount REQUIRED_AMOUNT = new Amount(1000, 0, "RUB");

  private final GoalRepository goalRepository = mock(GoalRepository.class);
  private final ConfigCommandSender configCommandSender = mock(
    ConfigCommandSender.class
  );
  private final UpdatedGoalSender updatedGoalSender = mock(
    UpdatedGoalSender.class
  );
  private final HistoryFacade historyFacade = mock(HistoryFacade.class);
  private final ConfigListener listener = new ConfigListener(
    goalRepository,
    configCommandSender,
    updatedGoalSender,
    historyFacade
  );

  private Goal goal(String id, String recipientId, String briefDescription) {
    var goalData = new GoalData(
      id,
      recipientId,
      WIDGET_ID,
      briefDescription,
      "Full description",
      new Amount(100, 0, "RUB"),
      REQUIRED_AMOUNT,
      true,
      GoalMode.CHOOSE
    );
    return new Goal(
      goalData,
      mock(GoalWidgetCommandSender.class),
      mock(GoalDataRepository.class),
      mock(GoalLinkRepository.class)
    );
  }

  private WidgetChangedEvent event(String type, Map<String, Object> goalConfig) {
    var property = new Widget.WidgetProperty(
      "goal",
      "Goals",
      "list",
      List.of(goalConfig)
    );
    var widget = new Widget(
      WIDGET_ID,
      "donationgoal",
      0,
      "widget",
      true,
      RECIPIENT_ID,
      false,
      new Widget.WidgetConfig(List.of(property))
    );
    return new WidgetChangedEvent(type, widget, "config", null);
  }

  private Map<String, Object> goalConfig() {
    var config = new HashMap<String, Object>();
    config.put("id", GOAL_ID);
    config.put("briefDescription", "Updated Goal");
    config.put("mode", "choose");
    config.put(
      "requiredAmount",
      Map.of("major", 1000, "minor", 0, "currency", "RUB")
    );
    config.put(
      "accumulatedAmount",
      Map.of("major", 100, "minor", 0, "currency", "RUB")
    );
    return config;
  }

  @Test
  @DisplayName("Should send GoalHistoryEvent for each updated goal")
  void listen_updatedEvent_sendsGoalHistoryEvent() {
    when(goalRepository.list(RECIPIENT_ID)).thenReturn(List.of());
    when(goalRepository.getById(GOAL_ID)).thenReturn(
      Optional.of(goal(GOAL_ID, RECIPIENT_ID, "Old Goal"))
    );

    listener.listen(event("updated", goalConfig()));

    verify(historyFacade).sendEvent(
      argThat(
        (GoalHistoryEvent e) ->
          "updated".equals(e.source()) &&
          GOAL_ID.equals(e.goalId()) &&
          WIDGET_ID.equals(e.widgetId()) &&
          RECIPIENT_ID.equals(e.recipientId()) &&
          "Updated Goal".equals(e.title()) &&
          Objects.equals(REQUIRED_AMOUNT, e.requiredAmount())
      )
    );
  }

  @Test
  @DisplayName("Should not send GoalHistoryEvent when goals are not updated")
  void listen_deletedEvent_noGoalHistoryEvent() {
    when(goalRepository.listByWidgetId(RECIPIENT_ID, WIDGET_ID)).thenReturn(
      List.of()
    );
    when(goalRepository.list(RECIPIENT_ID)).thenReturn(List.of());

    listener.listen(event("deleted", goalConfig()));

    verify(historyFacade, never()).sendEvent(any());
  }
}
