package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender.Stage;
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
public class ConfigListener {

  private static final String WIDGET_TYPE = "donationgoal";

  private final ODALogger log = new ODALogger(this);
  private final GoalRepository repository;
  private final ConfigCommandSender configCommandSender;
  private final UpdatedGoalSender updatedGoalSender;

  @Inject
  public ConfigListener(
    GoalRepository repository,
    ConfigCommandSender configCommandSender,
    UpdatedGoalSender updatedGoalSender
  ) {
    this.repository = repository;
    this.configCommandSender = configCommandSender;
    this.updatedGoalSender = updatedGoalSender;
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
    log.info("Loaded existing goals", Map.of("goals", savedGoals));

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
                .map(config -> {
                  var id = (String) config.get("id");
                  Goal updated = repository
                    .getById(id)
                    .orElseGet(() ->
                      repository.create(widget.ownerId(), widget.id(), id)
                    )
                    .update(widget.enabled(), config);
                  // TODO: batch
                  updatedGoalSender.sendGoal(Stage.SYNCED, updated.asUpdatedGoal());
                  return updated.save();
                })
                .toList()
            );
          }
        });
      log.info("Configuration changed", Map.of("goals", updatedGoals));

      savedGoals
        .stream()
        .filter(goal -> widget.id().equals(goal.widgetId()))
        .filter(goal -> {
          var result = updatedGoals
            .stream()
            .filter(updated -> updated.id().equals(goal.id()))
            .findAny()
            .isEmpty();
          log.info(
            "Checking goal for deletion",
            Map.of("goal", goal, "updatedGoals", updatedGoals, "result", result)
          );
          return result;
        })
        .forEach(Goal::delete);

      updatedGoalSender.sendGoal(
        Stage.SYNCED,
        new UpdatedGoal(
          "",
          widget.id(),
          widget.ownerId(),
          "",
          "",
          new Amount(0, 0, "RUB"),
          new Amount(0, 0, "RUB"),
          false
        )
      );
    }
  }
}
