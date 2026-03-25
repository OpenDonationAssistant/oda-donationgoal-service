package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;

public class HistoryItemEventHandler
  extends AbstractMessageHandler<HistoryItemEvent> {

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
      return;
    }
    repository
      .getByOriginId(originId)
      .ifPresent(goal ->
        facade.sendEvent(
          new GoalHistoryEvent(
            "payment",
            originId,
            goal.data().recipientId(),
            goal.data().widgetId(),
            goal.data().id(),
            Optional.ofNullable(goal.data().briefDescription()).orElse(""),
            goal.data().accumulatedAmount()
          )
        )
      );
  }
}
