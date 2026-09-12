package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalData;
import io.github.opendonationassistant.donationgoal.repository.GoalDataRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalLinkRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalMode;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.config.ConfigCommand;
import io.github.opendonationassistant.events.config.ConfigCommandSender;
import io.github.opendonationassistant.events.goal.GoalWidgetCommandSender;
import io.github.opendonationassistant.events.goal.UpdatedGoal;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender.Stage;
import io.github.opendonationassistant.events.widget.Widget;
import io.github.opendonationassistant.events.widget.WidgetCommandSender;
import io.github.opendonationassistant.events.widget.WidgetCommandSender.WidgetUpdateCommand;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.core.util.StringUtils;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RabbitListener
public class GoalListener {

  public static final String CALCULATED_GOALS = "goal.calculated";
  public static final String FINISHED_GOALS = "goal.finished";
  public static final io.github.opendonationassistant.rabbit.Queue CALCULATED_GOALS_QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(CALCULATED_GOALS);
  public static final io.github.opendonationassistant.rabbit.Queue FINISHED_GOALS_QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(FINISHED_GOALS);
  public static final List<Exchange> BINDING = List.of(
    Exchange.Exchange(
      "goals",
      Map.of("afterautomation", CALCULATED_GOALS_QUEUE)
    ),
    Exchange.Exchange("goals", Map.of("synced", FINISHED_GOALS_QUEUE))
  );

  private final ODALogger log = new ODALogger(this);
  private final ConfigCommandSender configCommandSender;
  private final GoalWidgetCommandSender goalCommandSender;
  private final WidgetCommandSender widgetCommandSender;
  private final GoalRepository repository;
  private final GoalDataRepository dataRepository;
  private final GoalLinkRepository linkRepository;
  private final UpdatedGoalSender goalSender;

  @Inject
  public GoalListener(
    ConfigCommandSender configCommandSender,
    GoalWidgetCommandSender goalCommandSender,
    WidgetCommandSender widgetCommandSender,
    GoalRepository repository,
    GoalDataRepository dataRepository,
    UpdatedGoalSender goalSender,
    GoalLinkRepository linkRepository
  ) {
    this.configCommandSender = configCommandSender;
    this.goalCommandSender = goalCommandSender;
    this.widgetCommandSender = widgetCommandSender;
    this.repository = repository;
    this.dataRepository = dataRepository;
    this.goalSender = goalSender;
    this.linkRepository = linkRepository;
  }

  @Queue(CALCULATED_GOALS)
  public void listen(UpdatedGoal update) {
    var mode = dataRepository
      .getById(update.goalId())
      .map(GoalData::mode)
      .orElseGet(() -> update.isDefault() ? GoalMode.DEFAULT : GoalMode.CHOOSE);
    var updated = new Goal(
      new GoalData(
        update.goalId(),
        update.recipientId(),
        update.widgetId(),
        update.briefDescription(),
        update.fullDescription(),
        update.accumulatedAmount(),
        update.requiredAmount(),
        true, // TODO: раз прилетел апдейт, значит донатгол активный
        mode
      ),
      goalCommandSender,
      dataRepository,
      linkRepository
    );
    updated.save();

    List<Goal> savedGoals = repository.list(update.recipientId());
    // обновление настроек виджета
    var goals = new Widget.WidgetProperty(
      "goal",
      "Цель",
      "",
      savedGoals
        .stream()
        .filter(goal -> goal.data().widgetId().equals(update.widgetId()))
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
    var patch = new Widget.WidgetConfig(List.of(goals));
    widgetCommandSender.send(new WidgetUpdateCommand(update.widgetId(), patch));

    goalSender.sendGoal(Stage.SYNCED, update);
  }

  @Queue(FINISHED_GOALS)
  public void listenFinished(UpdatedGoal update) {
    var goalMode = dataRepository
      .getById(update.goalId())
      .map(GoalData::mode)
      .orElseGet(() -> update.isDefault() ? GoalMode.DEFAULT : GoalMode.CHOOSE);
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
        goalMode
      ),
      goalCommandSender,
      dataRepository,
      linkRepository
    );
    List<Goal> savedGoals = repository.list(update.recipientId());
    log.info(
      "Reload all goals",
      Map.of("recipientId", update.recipientId(), "goals", savedGoals)
    );

    // обновление конфига страницы — skip for "all" mode
    if (goalMode != GoalMode.ALL) {
      savedGoals = savedGoals
        .stream()
        .filter(goal -> goal.data().enabled())
        .toList();
      // TODO fix nullable goals
      configCommandSender.send(
        new ConfigCommand.PutKeyValue(
          update.recipientId(),
          "paymentpage",
          "goals",
          savedGoals
            .stream()
            .map(Goal::data)
            .map(data -> {
              var map = new HashMap<String, Object>();
              map.put("id", data.id());
              map.put("recipientId", data.recipientId());
              map.put("widgetId", data.widgetId());
              map.put("briefDescription", data.briefDescription());
              map.put("fullDescription", data.fullDescription());
              map.put("accumulatedAmount", data.accumulatedAmount());
              map.put("requiredAmount", data.requiredAmount());
              map.put("enabled", data.enabled());
              map.put("isDefault", data.mode() == GoalMode.DEFAULT);
              return map;
            })
            .toList()
        )
      );
    }

    // TODO: send 1 message instead of 3 ( maybe use WidgetChangedNotification)
    // TODO: reload would be done without it, is it needed?
    log.info(
      "Send GoalCommand",
      Map.of("command", updated.asGoalCommand(), "update", update)
    );
    goalCommandSender.sendGoalCommand(
      update.recipientId(),
      updated.asGoalCommand()
    );

    // обновление для history-service
    if (!StringUtils.isEmpty(update.goalId())) {
      goalSender.sendGoal(Stage.FINALIZED, update);
    }
  }
}
