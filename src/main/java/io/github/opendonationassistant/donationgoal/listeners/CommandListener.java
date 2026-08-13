package io.github.opendonationassistant.donationgoal.listeners;

import static java.util.Optional.ofNullable;

import io.github.opendonationassistant.commons.Amount;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.GoalLinkRepository;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInDefaultGoalCommand;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender;
import io.github.opendonationassistant.events.goal.UpdatedGoalSender.Stage;
import io.github.opendonationassistant.events.history.event.DeletedHistoryItem;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.rabbit.Key;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class CommandListener {

  public static final String QUEUE_NAME = "goal.command";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final List<Exchange> BINDING = List.of(
    Exchange.Exchange("commands", Map.of(Key.COMMAND, QUEUE))
  );

  private final ODALogger log = new ODALogger(this);

  private final GoalRepository repository;
  private final UpdatedGoalSender goalSender;
  private final GoalLinkRepository linkRepository;

  @Inject
  public CommandListener(
    GoalRepository repository,
    UpdatedGoalSender goalSender,
    GoalLinkRepository linkRepository
  ) {
    this.repository = repository;
    this.goalSender = goalSender;
    this.linkRepository = linkRepository;
  }

  @Queue(value = QUEUE_NAME, executor = "command-listener")
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
                  .map(amount ->
                    goal.add(amount, "payment", command.paymentId())
                  )
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
          .flatMap(command ->
            ofNullable(command.recipientId())
              .flatMap(repository::getDefaultGoal)
              .map(goal -> {
                return ofNullable(command.amount())
                  .map(amount ->
                    goal.add(amount, "payment", command.paymentId())
                  )
                  .orElse(goal)
                  .asUpdatedGoal();
              })
          )
          .ifPresent(goal -> goalSender.sendGoal(Stage.AFTER_PAYMENT, goal));
        break;
      case "SetDefaultGoalAmount":
        var setAmountCommand = ObjectMapper.getDefault()
          .readValue(payload, SetDefaultGoalAmount.class);
        ofNullable(setAmountCommand)
          .flatMap(command ->
            ofNullable(command.recipientId())
              .flatMap(repository::getDefaultGoal)
              .map(goal ->
                ofNullable(command.amount())
                  .map(goal::setRequiredAmount)
                  .orElse(goal)
                  .save()
                  .asUpdatedGoal()
              )
          )
          .ifPresent(goal -> goalSender.sendGoal(Stage.AFTER_PAYMENT, goal));
        break;
      case "DeletedHistoryItem":
        var deletedItem = ObjectMapper.getDefault()
          .readValue(payload, DeletedHistoryItem.class);
        ofNullable(deletedItem).ifPresent(item -> {
          item
            .goals()
            .stream()
            .map(DeletedHistoryItem.Goal::goalId)
            .map(repository::getById)
            .flatMap(Optional::stream)
            .forEach(goal -> {
              log.info(
                "Processing deleted history item for goal",
                Map.of(
                  "goalId",
                  goal.data().id(),
                  "historyItemId",
                  item.historyItemId()
                )
              );
            });
          ofNullable(item.originId())
            .flatMap(linkRepository::getByOriginId)
            .ifPresent(link -> {
              log.info(
                "Deleting goal link for deleted history item",
                Map.of("linkId", link.id(), "originId", item.originId())
              );
              linkRepository.delete(link);
            });
        });
        break;
      default:
        log.info("Unknown command", Map.of("type", type));
        break;
    }
  }

  @Serdeable
  public static record SetDefaultGoalAmount(
    String recipientId,
    Amount amount
  ) {}
}
