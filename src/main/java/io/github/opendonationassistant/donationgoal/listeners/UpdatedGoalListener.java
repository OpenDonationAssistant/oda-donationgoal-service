package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalDataRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.config.ConfigPutCommand;
import io.github.opendonationassistant.events.goal.GoalCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.github.opendonationassistant.events.widget.WidgetCommandSender;
import io.github.opendonationassistant.events.widget.WidgetConfig;
import io.github.opendonationassistant.events.widget.WidgetProperty;
import io.github.opendonationassistant.events.widget.WidgetUpdateCommand;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@RabbitListener
public class UpdatedGoalListener {

  private final ConfigCommandSender configCommandSender;
  private final GoalCommandSender goalCommandSender;
  private final WidgetCommandSender widgetCommandSender;
  private final GoalRepository repository;
  private final GoalDataRepository dataRepository;

  @Inject
  public UpdatedGoalListener(
    ConfigCommandSender configCommandSender,
    GoalCommandSender goalCommandSender,
    WidgetCommandSender widgetCommandSender,
    GoalRepository repository,
    GoalDataRepository dataRepository
  ) {
    this.configCommandSender = configCommandSender;
    this.goalCommandSender = goalCommandSender;
    this.widgetCommandSender = widgetCommandSender;
    this.repository = repository;
    this.dataRepository = dataRepository;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Goal.FINISHED)
  public void listen(UpdatedGoal update) {
    var updated = new Goal(
      new GoalData(
        update.goalId(),
        update.recipientId(),
        update.widgetId(),
        update.briefDescription(),
        update.fullDescription(),
        update.accumulatedAmount(),
        update.requiredAmount(),
        true, // TODO: раз прилетел апдейт, сначит донатгол активный
        update.isDefault()
      ),
      goalCommandSender,
      dataRepository
    );
    updated.save();

    List<Goal> savedGoals = repository.list(update.recipientId());

    // обновление конфига страницы
    configCommandSender.send(
      new ConfigPutCommand(
        update.recipientId(),
        "paymentpage",
        "goals",
        savedGoals.stream().map(Goal::data).toList()
      )
    );

    // обновление настроек виджета
    var goals = new WidgetProperty(
      "goal",
      "Цель",
      "",
      savedGoals
        .stream()
        .filter(goal -> goal.widgetId().equals(update.widgetId()))
        .map(Goal::asWidgetConfigGoal)
        .reduce(
          new ArrayList<>(),
          (list, goal) -> {
            list.add(goal);
            return list;
          },
          (first, second) -> {
            first.addAll(second);
            return first;
          }
        )
    );
    var patch = new WidgetConfig(List.of(goals));
    widgetCommandSender.send(new WidgetUpdateCommand(update.widgetId(), patch));
  }
}
