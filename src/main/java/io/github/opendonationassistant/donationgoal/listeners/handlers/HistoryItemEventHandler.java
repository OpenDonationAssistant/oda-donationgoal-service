package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.MessageHandler;
import io.github.opendonationassistant.events.history.HistoryFacade;
import io.github.opendonationassistant.events.history.event.GoalHistoryEvent;
import io.github.opendonationassistant.events.history.event.HistoryItemEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;

public class HistoryItemEventHandler implements MessageHandler {

  private final ObjectMapper mapper;
  private final GoalRepository repository;
  private final HistoryFacade facade;

  @Inject
  public HistoryItemEventHandler(
    ObjectMapper mapper,
    GoalRepository repository,
    HistoryFacade facade
  ) {
    this.mapper = mapper;
    this.repository = repository;
    this.facade = facade;
  }

  @Override
  public void handle(byte[] message) throws IOException {
    final var item = mapper.readValue(message, HistoryItemEvent.class);
    if (item == null) {
      return;
    }
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
            goal.data().widgetId(),
            goal.data().id(),
            Optional.ofNullable(goal.data().briefDescription()).orElse("")
          )
        )
      );
  }

  @Override
  public String type() {
    return "HistoryItemEvent";
  }
}
