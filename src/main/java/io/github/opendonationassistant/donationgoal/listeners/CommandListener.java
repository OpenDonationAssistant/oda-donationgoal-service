package io.github.opendonationassistant.donationgoal.listeners;

import static java.util.Optional.ofNullable;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInDefaultGoalCommand;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender.Stage;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class CommandListener {

  private final ODALogger log = new ODALogger(this);

  private final GoalRepository repository;
  private final UpdatedGoalSender goalSender;

  @Inject
  public CommandListener(
    GoalRepository repository,
    UpdatedGoalSender goalSender
  ) {
    this.repository = repository;
    this.goalSender = goalSender;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Goal.COMMAND)
  public void listen(@MessageHeader String type, byte[] payload)
    throws IOException {
    switch (type) {
      case "CountPaymentInSpecifiedGoalCommand":
        var specifiedGoalCommand = ObjectMapper.getDefault()
          .readValue(payload, CountPaymentInSpecifiedGoalCommand.class);
        ofNullable(specifiedGoalCommand)
          .flatMap(command -> {
            return ofNullable(command.goalId())
              .flatMap(repository::getById)
              .map(goal ->
                ofNullable(command.amount())
                  .map(amount -> goal.add(amount))
                  .orElse(goal)
                  .asUpdatedGoal()
              );
          })
          .ifPresent(goal -> goalSender.sendGoal(Stage.AFTER_PAYMENT, goal));
        break;
      case "CountPaymentInDefaultGoalCommand":
        var defaultGoalCommand = ObjectMapper.getDefault()
          .readValue(payload, CountPaymentInDefaultGoalCommand.class);
        ofNullable(defaultGoalCommand)
          .flatMap(command -> {
            return ofNullable(command.recipientId())
              .flatMap(this::findDefaultGoal)
              .map(goal ->
                ofNullable(command.amount())
                  .map(amount -> goal.add(amount))
                  .orElse(goal)
                  .asUpdatedGoal()
              );
          })
          .ifPresent(goal -> goalSender.sendGoal(Stage.AFTER_PAYMENT, goal));
        break;
      default:
        log.info("Unknown command", Map.of("type", type));
        break;
    }
  }

  private Optional<Goal> findDefaultGoal(String recipientId) {
    return repository
      .list(recipientId)
      .stream()
      .filter(goal -> goal.data().enabled() && goal.data().isDefault())
      .findFirst();
  }
}
