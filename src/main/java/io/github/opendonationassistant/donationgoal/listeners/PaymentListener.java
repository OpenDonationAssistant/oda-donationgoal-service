package io.github.opendonationassistant.donationgoal.listeners;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.CompletedPaymentNotification;
import io.github.opendonationassistant.events.goal.GoalFacade;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

@RabbitListener
public class PaymentListener {

  private final ODALogger log = new ODALogger(this);

  private final GoalRepository repository;
  private final GoalFacade goalFacade;

  @Inject
  public PaymentListener(GoalRepository repository, GoalFacade goalFacade) {
    this.repository = repository;
    this.goalFacade = goalFacade;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Payments.GOAL)
  public void listen(CompletedPaymentNotification payment) {
    log.info("Received payment", Map.of("payment", payment));

    Optional.ofNullable(payment.goal())
      .flatMap(repository::getById)
      .ifPresent(goal ->
        goalFacade.run(
          new CountPaymentInSpecifiedGoalCommand(
            payment.id(),
            payment.recipientId(),
            goal.id(),
            payment.amount()
          )
        )
      );
  }
}
