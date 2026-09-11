package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class HistoryItemEventHandler
  extends AbstractMessageHandler<HistoryItemEvent> {

  private final ODALogger log = new ODALogger(this);
  private final GoalRepository repository;
  private final HistoryFacade facade;

  @Inject
  public HistoryItemEventHandler(
    ObjectMapper mapper,
    GoalRepository repository,
    HistoryFacade facade
  ) {
    this.repository = repository;
    this.facade = facade;
    super(mapper);
  }

  @Override
  public void handle(HistoryItemEvent item) throws IOException {
    final var originId = item.originId();
    if (originId == null) {
      log.debug("originId is null", Map.of("id", item.id()));
      return;
    }

    log.debug(
      "Searching linked goals by originId",
      Map.of("originId", originId)
    );
    repository
      .getByOriginId(originId)
      .forEach(goal -> sendGoalHistoryEvent(originId, item.type(), goal));
  }

  private void sendGoalHistoryEvent(String originId, String source, Goal goal) {
    facade.sendEvent(
      new GoalHistoryEvent(
        source,
        originId,
        goal.data().recipientId(),
        goal.data().widgetId(),
        goal.data().id(),
        Optional.ofNullable(goal.data().briefDescription()).orElse(""),
        goal.data().accumulatedAmount()
      )
    );
  }
}
