package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.config.ConfigPutCommand;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.events.widget.WidgetChangedEvent;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class DonationGoalWidgetChangesListener {

  private static final String WIDGET_TYPE = "donationgoal";

  private final ODALogger log = new ODALogger(this);
  private final GoalRepository repository;
  private final ConfigCommandSender configCommandSender;

  @Inject
  public DonationGoalWidgetChangesListener(
    GoalRepository repository,
    ConfigCommandSender configCommandSender
  ) {
    this.repository = repository;
    this.configCommandSender = configCommandSender;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Configs.GOAL)
  public void listen(WidgetChangedEvent event) {
    if (event == null) {
      return;
    }
    log.info("Received goals configuration", Map.of("event", event));
    Widget widget = event.widget();
    if (widget == null) {
      return;
    }
    if (!WIDGET_TYPE.equals(widget.type())) {
      return;
    }

    if ("deleted".equals(event.type())) {
      repository
        .listByWidgetId(widget.ownerId(), widget.id())
        .stream()
        .forEach(Goal::delete);
    }

    List<Goal> savedGoals = repository.list(widget.ownerId());

    if ("updated".equals(event.type()) || "toggled".equals(event.type())) {
      List<Goal> updatedGoals = new ArrayList<>();
      widget
        .config()
        .properties()
        .stream()
        .forEach(property -> {
          if ("goal".equals(property.name())) {
            var goals = (List<Map<String, Object>>) property.value();
            updatedGoals.addAll(
              Optional.ofNullable(goals)
                .orElse(List.of())
                .stream()
                .flatMap(config -> {
                  var id = (String) config.get("id");
                  return repository
                    .getById(id)
                    .map(found -> found.update(widget.enabled(), config))
                    .stream();
                })
                .toList()
            );
          }
        });
      savedGoals
        .stream()
        .filter(goal -> widget.id().equals(goal.id()))
        .filter(goal ->
          updatedGoals
            .stream()
            .filter(updated -> updated.id().equals(goal.id()))
            .findFirst()
            .isEmpty()
        )
        .forEach(Goal::delete);
    }

    savedGoals = repository
      .list(widget.ownerId())
      .stream()
      .filter(goal -> goal.isEnabled())
      .toList();

    configCommandSender.send(
      new ConfigPutCommand(widget.ownerId(), "paymentpage", "goals", savedGoals)
    );
  }
}
