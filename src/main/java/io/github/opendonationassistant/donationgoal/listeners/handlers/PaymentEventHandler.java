package io.github.opendonationassistant.donationgoal.listeners.handlers;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.donationgoal.repository.GoalRepository;
import io.github.opendonationassistant.events.MessageHandler;
import io.github.opendonationassistant.events.goal.GoalFacade;
import io.github.opendonationassistant.events.goal.GoalFacade.CountPaymentInSpecifiedGoalCommand;
import io.github.opendonationassistant.events.payments.PaymentEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;

public class PaymentEventHandler implements MessageHandler {

  private final ODALogger log = new ODALogger(this);
  private final ObjectMapper mapper;
  private final GoalFacade facade;
  private final GoalRepository repository;

  @Inject
  public PaymentEventHandler(
    ObjectMapper mapper,
    GoalFacade facade,
    GoalRepository repository
  ) {
    this.mapper = mapper;
    this.facade = facade;
    this.repository = repository;
  }

  @Override
  public void handle(byte[] message) throws IOException {
    final var payment = mapper.readValue(message, PaymentEvent.class);
    if (payment == null) {
      return;
    }
    final var goalId = payment.goal();
    if (goalId == null) {
      return;
    }
    log.debug("Received PaymentEvent", Map.of("payment", payment));
    repository
      .getById(payment.goal())
      .ifPresent(goal -> {
        goal.link(payment.id(), "payment");
        facade.run(
          new CountPaymentInSpecifiedGoalCommand(
            payment.id(),
            payment.recipientId(),
            goalId,
            payment.amount()
          )
        );
      });
  }

  @Override
  public String type() {
    return "PaymentEvent";
  }
}
