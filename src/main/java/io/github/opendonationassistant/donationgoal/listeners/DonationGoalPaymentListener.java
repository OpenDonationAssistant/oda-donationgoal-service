package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.Goal;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.CompletedPaymentNotification;
import io.github.opendonationassistant.events.goal.GoalSender;
import io.github.opendonationassistant.events.goal.GoalSender.Stage;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class DonationGoalPaymentListener {

  private final ODALogger log = new ODALogger(this);

  private final GoalRepository repository;
  private final GoalSender goalSender;

  @Inject
  public DonationGoalPaymentListener(
    GoalRepository repository,
    GoalSender goalSender
  ) {
    this.repository = repository;
    this.goalSender = goalSender;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Payments.GOAL)
  public void listen(CompletedPaymentNotification payment) {
    log.info("Received payment", Map.of("payment", payment));

    Optional.ofNullable(payment.goal())
      .flatMap(repository::getById)
      .or(() -> findDefaultGoal(payment.recipientId()))
      .map(goal -> goal.handlePayment(payment))
      .map(Goal::asUpdatedGoal)
      .ifPresent(goal -> goalSender.sendGoal(Stage.AFTER_PAYMENT, goal));
  }

  private Optional<Goal> findDefaultGoal(String recipientId) {
    return repository
      .list(recipientId)
      .stream()
      .filter(goal -> goal.data().isDefault())
      .findFirst();
  }
}
